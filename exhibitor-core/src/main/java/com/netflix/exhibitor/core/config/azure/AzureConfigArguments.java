package com.netflix.exhibitor.core.config.azure;

public class AzureConfigArguments {
    private final String        container;
    private final String        uri;
    private final AzureConfigAutoManageLockArguments lockArguments;

    public AzureConfigArguments(String container, String uri, AzureConfigAutoManageLockArguments lockArguments)
    {
        this.container = container;
        this.uri = uri;
        this.lockArguments = lockArguments;
    }

    public String getContainer()
    {
        return container;
    }

    public String getUri()
    {
        return uri;
    }

    public AzureConfigAutoManageLockArguments getLockArguments()
    {
        return lockArguments;
    }
}
