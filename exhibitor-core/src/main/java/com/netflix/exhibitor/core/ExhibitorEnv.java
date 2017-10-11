package com.netflix.exhibitor.core;

public interface ExhibitorEnv
{
    public static final String SERVER_KEYSTORE_PATH = "EXHIBITOR_TLS_SERVER_KEYSTORE_PATH";
    public static final String SERVER_KEYSTORE_PASSWORD = "EXHIBITOR_TLS_SERVER_KEYSTORE_PASSWORD";
    public static final String CLIENT_KEYSTORE_PATH = "EXHIBITOR_TLS_CLIENT_KEYSTORE_PATH";
    public static final String CLIENT_KEYSTORE_PASSWORD = "EXHIBITOR_TLS_CLIENT_KEYSTORE_PASSWORD";
    public static final String TRUSTSTORE_PATH = "EXHIBITOR_TLS_TRUSTSTORE_PATH";
    public static final String TRUSTSTORE_PASSWORD = "EXHIBITOR_TLS_TRUSTSTORE_PASSWORD";
    public static final String REQUIRE_CLIENT_CERT = "EXHIBITOR_TLS_REQUIRE_CLIENT_CERT";
    public static final String VERIFY_PEER_CERT = "EXHIBITOR_TLS_VERIFY_PEER_CERT";
}

