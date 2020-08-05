/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.exhibitor.core.automanage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.netflix.exhibitor.core.HttpsConfiguration;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;

/**
 * SSL Jetty server used for testing RemoteInstanceRequestHttpsClientImpl.java. It emulates as much
 * as possible the Jetty server embedded in ExhibitorMain.java. The embedded server could not be used
 * for testing for it requires an Exhibitor instance and hence all the command line parameters.
 */
public class JettyServer
{
    @Path("test")
    public static class RestService
    {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String test()
        {
            return "Test";
        }
    }

    private Server server;

    public JettyServer(int port, HttpsConfiguration httpsConf)
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

        server = new Server();
        ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setIdleTimeout(5000);
        connector.setPort(port);

        server.addConnector(connector);
        server.setStopAtShutdown(true);

        DefaultResourceConfig config = new DefaultResourceConfig(JettyServer.RestService.class);
        ServletContainer container = new ServletContainer(config);

        ServletContextHandler context = new ServletContextHandler(server, "/",  ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(container), "/*");
    }

    public void start() throws Exception
    {
        server.start();
    }

    public void stop() throws Exception
    {
        server.stop();
    }
}
