package com.netflix.exhibitor.core.gcs;

public interface GcsCredential {
    public String getAccountEmail();
    public String getAccountId();
    public String getPrivateKeyPath();
}
