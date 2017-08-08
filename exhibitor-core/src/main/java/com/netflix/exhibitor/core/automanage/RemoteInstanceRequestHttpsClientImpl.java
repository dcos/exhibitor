package com.netflix.exhibitor.core.automanage;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.exhibitor.core.RemoteConnectionConfiguration;
import com.netflix.exhibitor.core.HttpsConfiguration;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.SocketException;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.ws.rs.core.MediaType;

public class RemoteInstanceRequestHttpsClientImpl implements RemoteInstanceRequestClient
{
    private final Client client;
    private final LoadingCache<URI, WebResource> webResources = CacheBuilder.newBuilder().softValues()
            .build(new CacheLoader<URI, WebResource>()
            {
                @Override
                public WebResource load(URI remoteUri) throws Exception
                {
                    return client.resource(remoteUri);
                }
            });

    public RemoteInstanceRequestHttpsClientImpl(RemoteConnectionConfiguration configuration, HttpsConfiguration httpsConf) throws Exception
    {
        FileInputStream keystoreInputStream = null;
        if ( httpsConf.getClientKeystorePath() != null )
            keystoreInputStream = new FileInputStream(httpsConf.getClientKeystorePath());

        char[] keystorePassword = null;
        if ( httpsConf.getClientKeystorePassword() != null )
            keystorePassword = httpsConf.getClientKeystorePassword().toCharArray();

        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(keystoreInputStream, keystorePassword);

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keystore, keystorePassword);
        KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

        TrustManager[] trustManagers = null;
        if ( httpsConf.isVerifyPeerCert() == false )
        {
            TrustManager tm = new X509TrustManager()
            {
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
                {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
                {
                }

                public X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }
            };
            trustManagers = new TrustManager[] { tm };
        }
        else
        {
            // If EXHIBITOR_TLS_VERIFY_PEER_CERT has been set, the truststore
            // cannot be null
            FileInputStream truststoreInputStream = new FileInputStream(httpsConf.getTruststorePath());
            KeyStore truststore = KeyStore.getInstance(KeyStore.getDefaultType());
            truststore.load(truststoreInputStream, httpsConf.getTruststorePassword().toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(truststore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(keyManagers, trustManagers, new SecureRandom());

        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, sc));

        client = Client.create(config);
        client.setConnectTimeout(configuration.getConnectionTimeoutMs());
        client.setReadTimeout(configuration.getReadTimeoutMs());
        for ( ClientFilter filter : configuration.getFilters() )
        {
            client.addFilter(filter);
        }
    }

    @Override
    public <T> T getWebResource(URI remoteUri, MediaType type, Class<T> clazz) throws Exception
    {
        try
        {
            return webResources.get(remoteUri).accept(type).get(clazz);
        }
        catch (Exception e)
        {
            if (e.getCause() instanceof SocketException)
            {
                throw (SocketException) e.getCause();
            }

            throw e;
        }
    }

    @Override
    public void close() throws IOException
    {
        client.destroy();
    }
}
