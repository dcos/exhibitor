package com.netflix.exhibitor.core;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;

public class HttpsConfiguration
{
    private String serverKeystorePath;
    private String serverKeystorePassword;
    private String clientKeystorePath;
    private String clientKeystorePassword;
    private String truststorePath;
    private String truststorePassword;
    private boolean requireClientCert;
    private boolean verifyPeerCert;

    public static class Builder
    {
        private HttpsConfiguration httpsConf = new HttpsConfiguration();

        public Builder serverKeystorePath(String serverKeystorePath)
        {
            httpsConf = new HttpsConfiguration(serverKeystorePath, httpsConf.serverKeystorePassword,
                httpsConf.clientKeystorePath, httpsConf.clientKeystorePassword, httpsConf.truststorePath,
                httpsConf.truststorePassword, httpsConf.requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder serverKeystorePassword(String serverKeystorePassword)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, serverKeystorePassword,
                httpsConf.clientKeystorePath, httpsConf.clientKeystorePassword, httpsConf.truststorePath,
                httpsConf.truststorePassword, httpsConf.requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder clientKeystorePath(String clientKeystorePath)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, httpsConf.serverKeystorePassword,
                clientKeystorePath, httpsConf.clientKeystorePassword, httpsConf.truststorePath,
                httpsConf.truststorePassword, httpsConf.requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder clientKeystorePassword(String clientKeystorePassword)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, httpsConf.serverKeystorePassword,
                httpsConf.clientKeystorePath, clientKeystorePassword, httpsConf.truststorePath,
                httpsConf.truststorePassword, httpsConf.requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder truststorePath(String truststorePath)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, httpsConf.serverKeystorePassword,
                httpsConf.clientKeystorePath, httpsConf.clientKeystorePassword, truststorePath,
                httpsConf.truststorePassword, httpsConf.requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder truststorePassword(String truststorePassword)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, httpsConf.serverKeystorePassword,
                httpsConf.clientKeystorePath, httpsConf.clientKeystorePassword, httpsConf.truststorePath,
                truststorePassword, httpsConf.requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder requireClientCert(String requireClientCertString)
        {
            boolean requireClientCertValue;
            if ( requireClientCertString != null )
            {
                requireClientCertValue = Boolean.parseBoolean(requireClientCertString);
            }
            else
            {
                requireClientCertValue = httpsConf.verifyPeerCert;
            }

            return requireClientCert(requireClientCertValue);
        }

        public Builder requireClientCert(boolean requireClientCert)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, httpsConf.serverKeystorePassword,
                httpsConf.clientKeystorePath, httpsConf.clientKeystorePassword, httpsConf.truststorePath,
                httpsConf.truststorePassword, requireClientCert, httpsConf.verifyPeerCert);
            return this;
        }

        public Builder verifyPeerCert(String verifyPeerCertString)
        {
            boolean verifyPeerCertValue;
            if ( verifyPeerCertString != null )
            {
                verifyPeerCertValue = Boolean.parseBoolean(verifyPeerCertString);
            }
            else
            {
                verifyPeerCertValue = httpsConf.verifyPeerCert;
            }

            return verifyPeerCert(verifyPeerCertValue);
        }

        public Builder verifyPeerCert(boolean verifyPeerCert)
        {
            httpsConf = new HttpsConfiguration(httpsConf.serverKeystorePath, httpsConf.serverKeystorePassword,
                httpsConf.clientKeystorePath, httpsConf.clientKeystorePassword, httpsConf.truststorePath,
                httpsConf.truststorePassword, httpsConf.requireClientCert, verifyPeerCert);
            return this;
        }

        public HttpsConfiguration build()
        {
            Preconditions.checkArgument(!(httpsConf.serverKeystorePath == null ^ httpsConf.serverKeystorePassword == null),
                "Both or neither of EXHIBITOR_TLS_SERVER_KEYSTORE_PATH and EXHIBITOR_TLS_SERVER_KEYSTORE_PASSWORD must be specified.");
            Preconditions.checkArgument(!(httpsConf.clientKeystorePath == null ^ httpsConf.clientKeystorePassword == null),
                "Both or neither of EXHIBITOR_TLS_CLIENT_KEYSTORE_PATH and EXHIBITOR_TLS_CLIENT_KEYSTORE_PASSWORD must be specified.");
            Preconditions.checkArgument(!(httpsConf.truststorePath == null ^ httpsConf.truststorePassword == null),
                "Both or neither of EXHIBITOR_TLS_TRUSTSTORE_PATH and EXHIBITOR_TLS_TRUSTSTORE_PASSWORD must be specified.");
            Preconditions.checkArgument(httpsConf.serverKeystorePath == null || !httpsConf.verifyPeerCert
                || httpsConf.truststorePath != null,
                "Verify peer cert requires truststore path and password to be specified.");
            return httpsConf;
        }

        private Builder()
        {
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private HttpsConfiguration()
    {
        requireClientCert = true;
        verifyPeerCert = true;
    }

    public HttpsConfiguration(String serverKeystorePath, String serverKeystorePassword,
        String clientKeystorePath, String clientKeystorePassword, String truststorePath,
        String truststorePassword, boolean requireClientCert, boolean verifyPeerCert)
    {
        this.serverKeystorePath = serverKeystorePath;
        this.serverKeystorePassword = serverKeystorePassword;
        this.clientKeystorePath = clientKeystorePath;
        this.clientKeystorePassword = clientKeystorePassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.requireClientCert = requireClientCert;
        this.verifyPeerCert = verifyPeerCert;
    }

    public String getServerKeystorePath()
    {
        return serverKeystorePath;
    }

    public String getServerKeystorePassword()
    {
        return serverKeystorePassword;
    }

    public String getClientKeystorePath()
    {
        return clientKeystorePath;
    }

    public String getClientKeystorePassword()
    {
        return clientKeystorePassword;
    }

    public String getTruststorePath()
    {
        return truststorePath;
    }

    public String getTruststorePassword()
    {
        return truststorePassword;
    }

    public boolean isRequireClientCert()
    {
        return requireClientCert;
    }

    public boolean isVerifyPeerCert()
    {
        return verifyPeerCert;
    }
}
