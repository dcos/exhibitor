package com.netflix.exhibitor.core.gcs;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GcsClientImpl implements GcsClient {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final GcsCredential credentials;
    private static final String APPLICATION_NAME = "exhibitor";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public GcsClientImpl(GcsCredential credentials) {
        this.credentials = credentials;
    }

    private Credential authorize() throws Exception {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Set<String> scopes = new HashSet<String>();
        scopes.add(StorageScopes.DEVSTORAGE_FULL_CONTROL);
        scopes.add(StorageScopes.DEVSTORAGE_READ_ONLY);
        scopes.add(StorageScopes.DEVSTORAGE_READ_WRITE);
        return new GoogleCredential.Builder()
            .setTransport(httpTransport)
            .setJsonFactory(JSON_FACTORY)
            .setServiceAccountId(credentials.getAccountEmail())
            .setServiceAccountPrivateKeyFromP12File(new File(credentials.getPrivateKeyPath()))
            .setServiceAccountScopes(scopes)
            .build();
    }

    @Override
    public Storage getClient() throws Exception {
        Credential credential = authorize();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new Storage.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
    }

    @Override
    public ByteArrayOutputStream getObject(String bucketName, String objectName) throws Exception {
        BigInteger fileSize = getObjectMetadata(bucketName, objectName).getSize();
        ByteArrayOutputStream os = new ByteArrayOutputStream(fileSize.intValue());
        getClient().objects().get(bucketName, objectName).executeMediaAndDownloadTo(os);
        return os;
    }

    @Override
    public StorageObject getObjectMetadata(String bucketName, String objectName) throws Exception {
        return getClient().objects().get(bucketName, objectName).execute();
    }

    @Override
    public List<StorageObject> listObjects(String bucketName, String prefix) throws Exception {
        List<StorageObject> storageObjects = getClient().objects().list(bucketName).execute().getItems();
        List<StorageObject> matchingObjects = new ArrayList<StorageObject>();
        for (StorageObject storageObject : storageObjects){
            if (storageObject.getName().startsWith(prefix)) {
                matchingObjects.add(storageObject);
            }
        }
        return matchingObjects;
    }

    @Override
    public void putObject(byte[] bytes, String bucketName, String objectName) throws Exception {
        StorageObject metadata = new StorageObject().setName(objectName);
        ByteArrayContent content = new ByteArrayContent("text/plain", bytes);
        getClient().objects().insert(bucketName, metadata, content).execute();
    }

    @Override
    public void deleteObject(String bucketName, String objectName) throws Exception {
        getClient().objects().delete(bucketName, objectName).execute();
    }

    @Override
    public void close() throws IOException {

    }
}