package com.netflix.exhibitor.core.config.stat;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.config.ConfigCollection;
import com.netflix.exhibitor.core.config.ConfigProvider;
import com.netflix.exhibitor.core.config.LoadedInstanceConfig;
import com.netflix.exhibitor.core.config.PropertyBasedInstanceConfig;
import com.netflix.exhibitor.core.config.PseudoLock;

public class StaticConfigProvider implements ConfigProvider {
    private final String ensemble;

    private final Properties defaults;

    public StaticConfigProvider(String ensemble, Properties defaults) throws Exception {
        this.ensemble = ensemble;
        this.defaults = defaults;
    }

    @Override
    public void start() throws Exception {
        // NOP
    }

    @Override
    public void close() throws IOException {
        // NOP
    }

    @Override
    public LoadedInstanceConfig loadConfig() throws Exception {
        long lastModified = new Date(0L).getTime();
        Properties properties = new Properties();
        properties.setProperty("com.netflix.exhibitor.servers-spec", ensemble);
        properties.setProperty("com.netflix.exhibitor.auto-manage-instances", "0");
        PropertyBasedInstanceConfig config = new PropertyBasedInstanceConfig(properties, defaults);
        return new LoadedInstanceConfig(config, lastModified);
    }

    @Override
    public LoadedInstanceConfig storeConfig(ConfigCollection config, long compareVersion) throws Exception {
        // ignore config changes
        return loadConfig();
    }

    @Override
    public PseudoLock newPseudoLock() throws Exception {
        return new PseudoLock() {
            @Override
            public boolean lock(ActivityLog log, long maxWait, TimeUnit unit) throws Exception {
                return true;
            }

            @Override
            public void unlock() throws Exception {
            }
        };
    }
}
