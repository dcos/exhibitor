/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.exhibitor.application;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.netflix.exhibitor.servlet.ExhibitorServletFilter;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.ExhibitorArguments;
import com.netflix.exhibitor.core.HttpsConfiguration;
import com.netflix.exhibitor.core.RemoteConnectionConfiguration;
import com.netflix.exhibitor.core.backup.BackupProvider;
import com.netflix.exhibitor.core.config.ConfigProvider;
import com.netflix.exhibitor.core.rest.UIContext;
import com.netflix.exhibitor.core.rest.jersey.JerseySupport;
import com.netflix.exhibitor.standalone.ExhibitorCreator;
import com.netflix.exhibitor.standalone.ExhibitorCreatorExit;
import com.netflix.exhibitor.standalone.SecurityArguments;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.HTTPDigestAuthFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import org.apache.curator.utils.CloseableUtils;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.eclipse.jetty.util.ProcessorUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.DispatcherType;

public class ExhibitorMain implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Server server;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Exhibitor exhibitor;
    private final AtomicBoolean shutdownSignaled = new AtomicBoolean(false);
    private final Map<String, String> users = Maps.newHashMap();

    public static void main(String[] args) throws Exception
    {
        ExhibitorCreator creator;
        try
        {
            creator = new ExhibitorCreator(args);
        }
        catch ( ExhibitorCreatorExit exit )
        {
            if ( exit.getError() != null )
            {
                System.err.println(exit.getError());
            }

            exit.getCli().printHelp();
            return;
        }

        SecurityArguments securityArguments = new SecurityArguments(creator.getSecurityFile(), creator.getRealmSpec(), creator.getRemoteAuthSpec());
        ExhibitorMain exhibitorMain = new ExhibitorMain
        (
            creator.getBackupProvider(),
            creator.getConfigProvider(),
            creator.getBuilder(),
            creator.getSecurityHandler(),
            securityArguments
        );
        setShutdown(exhibitorMain);

        exhibitorMain.start();
        try
        {
            exhibitorMain.join();
        }
        finally
        {
            exhibitorMain.close();

            for ( Closeable closeable : creator.getCloseables() )
            {
                CloseableUtils.closeQuietly(closeable);
            }
        }
    }

    public ExhibitorMain(BackupProvider backupProvider, ConfigProvider configProvider, ExhibitorArguments.Builder builder, SecurityHandler security, SecurityArguments securityArguments) throws Exception
    {
        HashLoginService loginService = makeLoginService(securityArguments);
        if ( securityArguments.getRemoteAuthSpec() != null )
        {
            addRemoteAuth(builder, securityArguments.getRemoteAuthSpec());
        }

        builder.shutdownProc(makeShutdownProc(this));
        exhibitor = new Exhibitor(configProvider, null, backupProvider, builder.build());
        exhibitor.start();

        DefaultResourceConfig   application = JerseySupport.newApplicationConfig(new UIContext(exhibitor));
        ServletContainer        container = new ServletContainer(application);

        // The server's threadPool implementation defaults to the
        // QueuedThreadPool.  The QueuedThreadPool has no limit on the
        // length of the queue. This means that in a scenario where
        // requests are arriving faster than they can be served, the
        // queue length grows as long as that state is
        // maintained. Additionally, once the request rate drops, the
        // backlog is serviced first, which means that stopping the
        // source of requests doesn't lead to the service becoming
        // responsive again promptly.
        //
        // Jetty 6.1.22 includes a BoundedThreadPool which on first
        // inspection seems to address this issue but it does not. The
        // 'Bounded' part refers to the size of the thread pool, not
        // to the length of the task queue.
        //
        // As such we have back-ported the ExecutorThreadPool. The
        // ExecutorThreadPool supports bounding the number of threads
        // in the threadPool and also setting a custom task queue. For
        // this queue we use the LinkedBlockingQueue which allows one
        // to limit the length of the queue. Requests arriving when
        // the queue is full will be refused.
        // See https://dcosjira.atlassian.net/browse/DCOS-558
        final int maxQueueSize = 4096;
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(maxQueueSize);

        // minThreads needs to be much higher than the number of Acceptors
        // See https://jira.mesosphere.com/browse/DCOS-14045
        // Calculate acceptors based on Processors as is done by default.
        int cores = ProcessorUtils.availableProcessors();
        int acceptors = Math.max(1, Math.min(4, cores / 8));
        final int minThreads = acceptors * 5;
        final int maxThreads = 100;

        ExecutorThreadPool threadPool = new ExecutorThreadPool(maxThreads, minThreads, queue);
        threadPool.setIdleTimeout(5000);
        server = new Server(threadPool);

        HttpsConfiguration httpsConf = exhibitor.getHttpsConfiguration();

        ConnectionFactory[] factory = {new HttpConnectionFactory()};

        if ( httpsConf.getServerKeystorePath() != null )
        {
            SslContextFactory sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(httpsConf.getServerKeystorePath());
            sslContextFactory.setKeyStorePassword(httpsConf.getServerKeystorePassword());

            if ( httpsConf.isVerifyPeerCert() )
            {
                sslContextFactory.setTrustStorePath(httpsConf.getTruststorePath());
                sslContextFactory.setTrustStorePassword(httpsConf.getTruststorePassword());
                sslContextFactory.setNeedClientAuth(true);
            }

            sslContextFactory.setWantClientAuth(httpsConf.isRequireClientCert());

            factory = AbstractConnectionFactory.getFactories(sslContextFactory, factory);
        }

        ServerConnector connector = new ServerConnector(server, factory);
        connector.setPort(exhibitor.getRestPort());
        connector.setIdleTimeout(5000);
        server.addConnector(connector);
        if (loginService != null)
        {
            server.addBean(loginService);
        }

        WebAppContext root = new WebAppContext();
        root.setContextPath("/");

        // NOTE(jkoelker) A war or path is required. Set to a non-existant
        //                path.
        root.setResourceBase("src/main/webapp/");

        root.addFilter(ExhibitorServletFilter.class, "/",  EnumSet.of(DispatcherType.INCLUDE,DispatcherType.REQUEST));
        root.addServlet(new ServletHolder(container), "/*");

        if ( security != null )
        {
            root.setSecurityHandler(security);
        }
        else if ( securityArguments.getSecurityFile() != null )
        {
            root.addOverrideDescriptor(securityArguments.getSecurityFile());
        }

        server.setHandler(root);
    }

    private void addRemoteAuth(ExhibitorArguments.Builder builder, String remoteAuthSpec)
    {

        String[] parts = remoteAuthSpec.split(":");
        Preconditions.checkArgument(parts.length == 2, "Badly formed remote client authorization: " + remoteAuthSpec);

        String type = parts[0].trim();
        String userName = parts[1].trim();

        String password = Preconditions.checkNotNull(users.get(userName), "Realm user not found: " + userName);

        ClientFilter filter;
        if ( type.equals("basic") )
        {
            filter = new HTTPBasicAuthFilter(userName, password);
        }
        else if ( type.equals("digest") )
        {
            filter = new HTTPDigestAuthFilter(userName, password);
        }
        else
        {
            throw new IllegalStateException("Unknown remote client authorization type: " + type);
        }

        builder.remoteConnectionConfiguration(new RemoteConnectionConfiguration(Arrays.asList(filter)));
    }

    public void start() throws Exception
    {
        server.start();
    }

    public void join()
    {
        try
        {
            while ( !shutdownSignaled.get() && !Thread.currentThread().isInterrupted() )
            {
                Thread.sleep(5000);
            }
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws IOException
    {
        if ( isClosed.compareAndSet(false, true) )
        {
            log.info("Shutting down");

            CloseableUtils.closeQuietly(exhibitor);
            try
            {
                server.stop();
            }
            catch ( Exception e )
            {
                log.error("Error shutting down Jetty", e);
            }
            server.destroy();
        }
    }

    private static void setShutdown(final ExhibitorMain exhibitorMain)
    {
        Runtime.getRuntime().addShutdownHook
        (
            new Thread
            (
                makeShutdownProc(exhibitorMain)
            )
        );
    }

    private static Runnable makeShutdownProc(final ExhibitorMain exhibitorMain)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                exhibitorMain.shutdownSignaled.set(true);
            }
        };
    }

    private HashLoginService makeLoginService(SecurityArguments securityArguments) throws Exception
    {
        if ( securityArguments.getRealmSpec() == null )
        {
            return null;
        }

        String[] parts = securityArguments.getRealmSpec().split(":");
        if ( parts.length != 2 )
        {
            throw new Exception("Bad realm spec: " + securityArguments.getRealmSpec());
        }

        PropertyUserStore propertyUserStore = new PropertyUserStore();
        propertyUserStore.setConfig(parts[1].trim());

        Resource config = propertyUserStore.getConfigResource();
        Properties properties = new Properties();
        properties.load(config.getInputStream());

        for (Map.Entry<Object, Object> entry : properties.entrySet())
        {
            String username = ((String)entry.getKey()).trim();
            String credentials = ((String)entry.getValue()).trim();
            String roles = null;
            int c = credentials.indexOf(',');
            if (c >= 0)
            {
                roles = credentials.substring(c + 1).trim();
                credentials = credentials.substring(0, c).trim();
            }

            if (username.length() > 0)
            {
                users.put(username, credentials);
            }
        }

        HashLoginService loginService = new HashLoginService(parts[0].trim());
        loginService.setUserStore(propertyUserStore);
        return loginService;
    }
}
