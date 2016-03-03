package com.netflix.exhibitor.core.gcs;


public class GcsClientFactoryImpl implements GcsClientFactory {
    @Override
    public GcsClient makeNewClient() throws Exception {
        return new GcsClientImpl();
    }
}