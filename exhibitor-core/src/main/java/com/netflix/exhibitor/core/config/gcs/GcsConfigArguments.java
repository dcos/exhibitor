package com.netflix.exhibitor.core.config.gcs;

public class GcsConfigArguments {
    private final String bucketName;
    private final String objectName;
    private final GcsConfigAutoManageLockArguments lockArguments;

    public GcsConfigArguments(String bucketName, String objectName, GcsConfigAutoManageLockArguments lockArguments) {
        this.bucketName = bucketName;
        this.objectName = objectName;
        this.lockArguments = lockArguments;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getObjectName() {
        return objectName;
    }

    public GcsConfigAutoManageLockArguments getLockArguments() {
        return lockArguments;
    }
}