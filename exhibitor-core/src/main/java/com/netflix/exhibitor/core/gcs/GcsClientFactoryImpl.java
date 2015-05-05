package com.netflix.exhibitor.core.gcs;


public class GcsClientFactoryImpl implements GcsClientFactory {
    @Override
    public GcsClient makeNewClient(GcsCredential credentials) throws Exception {
        return new GcsClientImpl(credentials);
    }
}