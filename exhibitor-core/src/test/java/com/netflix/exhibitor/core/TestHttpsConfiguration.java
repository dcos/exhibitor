package com.netflix.exhibitor.core;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestHttpsConfiguration
{
    @Test
    public void testEmptyConfig()
    {
        HttpsConfiguration.builder().build();
    }

    @Test(expectedExceptions = {Exception.class})
    public void testNoServerKeystorePass() throws Exception
    {
        try
        {
            HttpsConfiguration.builder()
                .serverKeystorePath("sPath").build();
        }
        catch ( Exception e )
        {
            if ( e.getMessage().contains("EXHIBITOR_TLS_SERVER_KEYSTORE_PATH and EXHIBITOR_TLS_SERVER_KEYSTORE_PASSWORD") )
            {
                throw new Exception();
            }
        }
    }

    @Test(expectedExceptions = {Exception.class})
    public void testNoTruststore() throws Exception
    {
        try
        {
            HttpsConfiguration.builder()
                .serverKeystorePath("sPath")
                .serverKeystorePassword("sPass")
                .build();
        }
        catch ( Exception e )
        {
            if ( e.getMessage().contains("Verify peer cert") )
            {
                throw new Exception();
            }
        }
    }

    @Test(expectedExceptions = {Exception.class})
    public void testNoClientPass() throws Exception
    {
        try
        {
            HttpsConfiguration.builder()
                .serverKeystorePath("sPath")
                .serverKeystorePassword("sPass")
                .clientKeystorePath("cPath")
                .verifyPeerCert("False")
                .build();
        }
        catch ( Exception e )
        {
            if ( e.getMessage().contains("EXHIBITOR_TLS_CLIENT_KEYSTORE_PATH and EXHIBITOR_TLS_CLIENT_KEYSTORE_PASSWORD") )
            {
                throw new Exception();
            }
        }
    }

    @Test(expectedExceptions = {Exception.class})
    public void testNoTrustPass() throws Exception
    {
        try
        {
            HttpsConfiguration.builder()
                .serverKeystorePath("sPath")
                .serverKeystorePassword("sPass")
                .truststorePath("tPath")
                .verifyPeerCert("False")
                .build();
        }
        catch ( Exception e )
        {
            if ( e.getMessage().contains("EXHIBITOR_TLS_TRUSTSTORE_PATH and EXHIBITOR_TLS_TRUSTSTORE_PASSWORD") )
            {
                throw new Exception();
            }
        }
    }

    @Test
    public void testNoClientKeystoreNoTruststore()
    {
        HttpsConfiguration.builder()
            .serverKeystorePath("sPath")
            .serverKeystorePassword("sPass")
            .verifyPeerCert("False")
            .build();
    }

    @Test void testNoClentKeystore()
    {
        HttpsConfiguration.builder()
            .serverKeystorePath("sPath")
            .serverKeystorePassword("sPass")
            .truststorePath("tPath")
            .truststorePassword("tPass")
            .verifyPeerCert("False")
            .build();
    }

    @Test
    public void testGetters()
    {
        HttpsConfiguration config = HttpsConfiguration.builder()
            .serverKeystorePath("sPath")
            .serverKeystorePassword("sPass")
            .clientKeystorePath("cPath")
            .clientKeystorePassword("cPass")
            .truststorePath("tPath")
            .truststorePassword("tPass")
            .requireClientCert("False")
            .verifyPeerCert("False")
            .build();

        Assert.assertEquals(config.getServerKeystorePath(), "sPath");
        Assert.assertEquals(config.getServerKeystorePassword(), "sPass");
        Assert.assertEquals(config.getClientKeystorePath(), "cPath");
        Assert.assertEquals(config.getClientKeystorePassword(), "cPass");
        Assert.assertEquals(config.getTruststorePath(), "tPath");
        Assert.assertEquals(config.getTruststorePassword(), "tPass");
        Assert.assertFalse(config.isRequireClientCert());
        Assert.assertFalse(config.isVerifyPeerCert());
    }
}