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

import com.netflix.exhibitor.core.HttpsConfiguration;
import com.netflix.exhibitor.core.RemoteConnectionConfiguration;

import org.apache.curator.test.InstanceSpec;
import org.apache.curator.utils.CloseableUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class TestRemoteInstanceRequestClient
{
    // Clientstore content:
    // 1 Certificate, CN: Client, SA Names: {localhost}, Signed by: Root, Valid until: August/2027
    private URL clientstoreUrl = ClassLoader.getSystemResource("com/netflix/exhibitor/core/automanage/test-clientstore");
    private String clientKeystorePath = clientstoreUrl.getPath();
    private String clientKeystorePassword = "password";

    // Serverstore content:
    // 1 Certificate, CN: Server, SA Names: {localhost}, Signed by: Root, Valid until: August/2027
    private URL serverstoreUrl = ClassLoader.getSystemResource("com/netflix/exhibitor/core/automanage/test-serverstore");
    private String serverKeystorePath = serverstoreUrl.getPath();
    private String serverKeystorePassword = "password";

    // Truststore content:
    // 1 Certificate, CN: Root, SA Names: {localhost}, Signed by: self, Valid until: August/2027
    URL validTruststoreUrl = ClassLoader.getSystemResource("com/netflix/exhibitor/core/automanage/test-validtruststore");
    String validTruststorePath = validTruststoreUrl.getPath();
    String validTruststorePassword = "password";

    // Truststore content:
    // 1 Certificate, CN: Root, SA Names: {localhost}, Signed by: self, Valid until: August/2027
    URL invalidTruststoreUrl = ClassLoader.getSystemResource("com/netflix/exhibitor/core/automanage/test-invalidTruststore");
    String invalidTruststorePath = invalidTruststoreUrl.getPath();
    String invalidTruststorePassword = "password";

    HttpsConfiguration.Builder builder = HttpsConfiguration.builder()
        .serverKeystorePath(serverKeystorePath)
        .serverKeystorePassword(serverKeystorePassword)
        .clientKeystorePath(clientKeystorePath)
        .clientKeystorePassword(clientKeystorePassword);

    @DataProvider(name = "pass")
    public Object[][] httpsConfigPass()
    {
        return new Object[][]
        {
            {
                builder.truststorePath(null)
                    .truststorePassword(null)
                    .requireClientCert("False")
                    .verifyPeerCert("False")
                    .build()
            },
            {
                builder.truststorePath(null)
                    .truststorePassword(null)
                    .requireClientCert("True")
                    .verifyPeerCert("False")
                    .build()
            },
            {
                builder.truststorePath(validTruststorePath)
                    .truststorePassword(validTruststorePassword)
                    .requireClientCert("True")
                    .verifyPeerCert("True")
                    .build()
            },
            {
                builder.truststorePath(validTruststorePath)
                    .truststorePassword(validTruststorePassword)
                    .requireClientCert("True")
                    .verifyPeerCert("False")
                    .build()
            },
            {
                builder.truststorePath(validTruststorePath)
                    .truststorePassword(validTruststorePassword)
                    .requireClientCert("False")
                    .verifyPeerCert("True")
                    .build()
            },
            {
                builder.truststorePath(invalidTruststorePath)
                    .truststorePassword(invalidTruststorePassword)
                    .requireClientCert("True")
                    .verifyPeerCert("False")
                    .build()
            },
            {
                builder.truststorePath(invalidTruststorePath)
                    .truststorePassword(invalidTruststorePassword)
                    .requireClientCert("False")
                    .verifyPeerCert("False")
                    .build()
            },
        };
    }

    @DataProvider(name = "fail")
    public Object[][] httpsConfigFail()
    {
        return new Object[][]
        {
            {
                builder.truststorePath(invalidTruststorePath)
                    .truststorePassword(invalidTruststorePassword)
                    .requireClientCert("False")
                    .verifyPeerCert("True")
                    .build()
            },
            {
                builder.truststorePath(invalidTruststorePath)
                    .truststorePassword(invalidTruststorePassword)
                    .requireClientCert("True")
                    .verifyPeerCert("True")
                    .build()
            },
        };
    }

    @Test
    public void     testMissingServer() throws URISyntaxException
    {
        RemoteInstanceRequestClientImpl         client = new RemoteInstanceRequestClientImpl(new RemoteConnectionConfiguration());
        try
        {
            // a non-existent port should generate an exception
            client.getWebResource(new URI("http://localhost:" + InstanceSpec.getRandomPort()), MediaType.WILDCARD_TYPE, Object.class);
        }
        catch ( Exception e )
        {
            Throwable cause = e.getCause();
            if ( cause == null )
            {
                cause = e;
            }
            Assert.assertTrue(cause instanceof ConnectException, cause.getClass().getName());
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
        }
    }

    @Test
    public void     testConnectionTimeout() throws Exception
    {
        int             port = InstanceSpec.getRandomPort();

        RemoteInstanceRequestClientImpl client = null;
        ServerSocket                    server = new ServerSocket(port, 0);
        try
        {
            client = new RemoteInstanceRequestClientImpl(new RemoteConnectionConfiguration());
            client.getWebResource(new URI("http://localhost:" + port), MediaType.WILDCARD_TYPE, Object.class);
        }
        catch ( Exception e )
        {
            Throwable cause = e.getCause();
            Assert.assertTrue(cause instanceof SocketTimeoutException);
        }
        finally
        {
            CloseableUtils.closeQuietly(client);
            server.close();
        }
    }

    @Test(dataProvider = "pass")
    public void testHttpsPass(HttpsConfiguration httpsConf) throws Exception
    {
        int port = InstanceSpec.getRandomPort();
        JettyServer server = new JettyServer(port, httpsConf);
        server.start();

        RemoteInstanceRequestHttpsClientImpl client = null;
        try
        {
            client = new RemoteInstanceRequestHttpsClientImpl(new RemoteConnectionConfiguration(), httpsConf);
            client.getWebResource(new URI("https://localhost:" + port + "/test"), MediaType.TEXT_PLAIN_TYPE, String.class);
        }
        finally
        {
            client.close();
            server.stop();
        }
    }

    @Test(dataProvider = "fail", expectedExceptions = {Exception.class})
    public void testHttpsFail(HttpsConfiguration httpsConf) throws Exception
    {
        int port = InstanceSpec.getRandomPort();
        JettyServer server = new JettyServer(port, httpsConf);
        server.start();

        RemoteInstanceRequestHttpsClientImpl client = null;
        try
        {
            client = new RemoteInstanceRequestHttpsClientImpl(new RemoteConnectionConfiguration(), httpsConf);
            client.getWebResource(new URI("https://localhost:" + port + "/test"), MediaType.TEXT_PLAIN_TYPE, String.class);
        }
        finally
        {
            client.close();
            server.stop();
        }
    }
}
