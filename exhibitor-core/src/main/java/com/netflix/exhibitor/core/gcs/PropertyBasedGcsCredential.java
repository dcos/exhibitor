package com.netflix.exhibitor.core.gcs;

import org.apache.curator.utils.CloseableUtils;

import java.io.*;
import java.util.Properties;

public class PropertyBasedGcsCredential implements GcsCredential {
    private final String accountEmail;
    private final String accountId;
    private final String privateKeyPath;

    public static final String PROPERTY_GCS_ACCOUNT_EMAIL = "com.netflix.exhibitor.gcs.account-email";
    public static final String PROPERTY_GCS_ACCOUNT_ID = "com.netflix.exhibitor.gcs.account-id";
    public static final String PROPERTY_GCS_PRIVATE_KEY_PATH = "com.netflix.exhibitor.gcs.private-key-path";

    public PropertyBasedGcsCredential(File propertiesFile) throws IOException {
        this(loadProperties(propertiesFile));
    }

    public PropertyBasedGcsCredential(Properties properties) {
        accountEmail = properties.getProperty(PROPERTY_GCS_ACCOUNT_EMAIL);
        accountId = properties.getProperty(PROPERTY_GCS_ACCOUNT_ID);
        privateKeyPath = properties.getProperty(PROPERTY_GCS_PRIVATE_KEY_PATH);
    }

    public String getAccountEmail()
    {
        return accountEmail;
    }

    public String getAccountId()
    {
        return accountId;
    }

    public String getPrivateKeyPath()
    {
        return privateKeyPath;
    }

    private static Properties loadProperties(File propertiesFile) throws IOException {
        Properties properties = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(propertiesFile));
        try {
            properties.load(in);
        } finally {
            CloseableUtils.closeQuietly(in);
        }
        return properties;
    }
}