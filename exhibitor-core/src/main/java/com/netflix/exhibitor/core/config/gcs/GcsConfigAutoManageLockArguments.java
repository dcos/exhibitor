package com.netflix.exhibitor.core.config.gcs;

import com.netflix.exhibitor.core.config.AutoManageLockArguments;
import java.util.concurrent.TimeUnit;

public class GcsConfigAutoManageLockArguments extends AutoManageLockArguments
{
    private final int settlingMs;

    public GcsConfigAutoManageLockArguments(String prefix) {
        super(prefix);
        settlingMs = (int)TimeUnit.SECONDS.toMillis(5); // TODO - get this right
    }

    public GcsConfigAutoManageLockArguments(String prefix, int timeoutMs, int pollingMs, int settlingMs) {
        super(prefix, timeoutMs, pollingMs);
        this.settlingMs = settlingMs;
    }

    public int getSettlingMs() {
        return settlingMs;
    }
}