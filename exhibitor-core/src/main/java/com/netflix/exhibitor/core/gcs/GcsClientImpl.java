package com.netflix.exhibitor.core.gcs;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.compute.ComputeCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.StorageObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class GcsClientImpl implements GcsClient {
    private static final String TOKEN_URI = "http://metadata/computeMetadata/v1/instance/service-accounts/default/token";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APPLICATION_NAME = "exhibitor";

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    CloseableHttpClient httpclient = HttpClients.createDefault();

    private String getAccessToken() throws IOException {
        HttpGet tokenGetRequest = new HttpGet(TOKEN_URI);
        tokenGetRequest.addHeader("Metadata-Flavor", "Google");
        CloseableHttpResponse httpResponse = httpclient.execute(tokenGetRequest);

        String content =  EntityUtils.toString(httpResponse.getEntity());
        if (StringUtils.isEmpty(content)) {
            throw new IOException("Failed to write entity content.");
        }
        JSONObject obj = new JSONObject(content);
        String token = obj.getString("access_token");
        return token;
    }

    private Credential authorize() throws Exception {
        JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        return new ComputeCredential.Builder(httpTransport, JSON_FACTORY).build();
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