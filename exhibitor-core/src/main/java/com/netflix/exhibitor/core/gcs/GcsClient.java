package com.netflix.exhibitor.core.gcs;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.StorageObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.List;

public interface GcsClient extends Closeable {
    public Storage getClient() throws Exception;

    public ByteArrayOutputStream getObject(String bucketName, String objectName) throws Exception;

    public StorageObject getObjectMetadata(String bucketName, String objectName) throws Exception;

    public List<StorageObject> listObjects(String bucketName, String prefix) throws Exception;

    public void putObject(byte[] bytes, String bucketName, String objectName) throws Exception;

    public void deleteObject(String bucketName, String objectName) throws Exception;
}