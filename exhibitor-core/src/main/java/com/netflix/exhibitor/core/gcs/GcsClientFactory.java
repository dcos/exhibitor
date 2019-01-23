package com.netflix.exhibitor.core.gcs;

public interface GcsClientFactory {

    /**
     * Create a client with the given credentials
     *
     * @return client
     * @throws Exception errors
     */
    public GcsClient makeNewClient() throws Exception;

}