package me.catcoder;

import com.vk.api.sdk.client.ClientResponse;
import com.vk.api.sdk.client.TransportClient;
import com.vk.api.sdk.httpclient.ConnectionsSupervisor;
import com.vk.api.sdk.httpclient.HttpDeleteWithBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents modified (without logging) {@link com.vk.api.sdk.httpclient.HttpTransportClient}
 *
 * @author CatCoder
 * @author Anton Tsivarev
 */
public class ModifiedHttpClient implements TransportClient {


    private static final String ENCODING = "UTF-8";
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String USER_AGENT = "Java VK SDK/0.5.8";

    private static final String EMPTY_PAYLOAD = "-";

    private static final int MAX_SIMULTANEOUS_CONNECTIONS = 300;
    private static final int DEFAULT_RETRY_ATTEMPTS_NETWORK_ERROR_COUNT = 3;
    private static final int FULL_CONNECTION_TIMEOUT_S = 60;
    private static final int CONNECTION_TIMEOUT_MS = 5_000;
    private static final int SOCKET_TIMEOUT_MS = FULL_CONNECTION_TIMEOUT_S * 1000;

    private static ModifiedHttpClient instance;
    private static HttpClient httpClient;

    private int retryAttemptsNetworkErrorCount;

    public ModifiedHttpClient() {
        this(DEFAULT_RETRY_ATTEMPTS_NETWORK_ERROR_COUNT);
    }

    public ModifiedHttpClient(int retryAttemptsNetworkErrorCount) {
        this.retryAttemptsNetworkErrorCount = retryAttemptsNetworkErrorCount;

        CookieStore cookieStore = new BasicCookieStore();
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

        connectionManager.setMaxTotal(MAX_SIMULTANEOUS_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(MAX_SIMULTANEOUS_CONNECTIONS);

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore)
                .setUserAgent(USER_AGENT)
                .build();
    }

    public static ModifiedHttpClient getInstance() {
        if (instance == null) {
            instance = new ModifiedHttpClient();
        }

        return instance;
    }

    private static Map<String, String> getHeaders(Header[] headers) {
        Map<String, String> result = new HashMap<>();
        for (Header header : headers) {
            result.put(header.getName(), header.getValue());
        }

        return result;
    }

    private ClientResponse call(HttpRequestBase request) throws IOException {
        SocketException exception = null;
        for (int i = 0; i < retryAttemptsNetworkErrorCount; i++) {
            HttpResponse response = httpClient.execute(request);
            try (InputStream content = response.getEntity().getContent()) {
                String result = IOUtils.toString(content, ENCODING);
                Map<String, String> responseHeaders = getHeaders(response.getAllHeaders());
                return new ClientResponse(response.getStatusLine().getStatusCode(), result, responseHeaders);
            } catch (SocketException e) {
                exception = e;
            }
        }

        assert exception != null;
        throw exception;
    }


    private String getRequestPayload(HttpRequestBase request) throws IOException {
        if (!(request instanceof HttpPost)) {
            return EMPTY_PAYLOAD;
        }

        HttpPost postRequest = (HttpPost) request;
        if (postRequest.getEntity() == null) {
            return EMPTY_PAYLOAD;
        }

        if (StringUtils.isNotEmpty(postRequest.getEntity().getContentType().getValue())) {
            String contentType = postRequest.getEntity().getContentType().getValue();
            if (contentType.contains("multipart/form-data")) {
                return EMPTY_PAYLOAD;
            }
        }

        return IOUtils.toString(postRequest.getEntity().getContent(), StandardCharsets.UTF_8);
    }


    @Override
    public ClientResponse get(String url) throws IOException {
        return get(url, FORM_CONTENT_TYPE);
    }

    @Override
    public ClientResponse get(String url, String contentType) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader(CONTENT_TYPE_HEADER, contentType);
        return call(request);
    }

    @Override
    public ClientResponse post(String url) throws IOException {
        return post(url, null);
    }

    @Override
    public ClientResponse post(String url, String body) throws IOException {
        return post(url, body, FORM_CONTENT_TYPE);
    }

    @Override
    public ClientResponse post(String url, String body, String contentType) throws IOException {
        HttpPost request = new HttpPost(url);
        request.setHeader(CONTENT_TYPE_HEADER, contentType);
        if (body != null) {
            request.setEntity(new StringEntity(body, "UTF-8"));
        }

        return call(request);
    }

    @Override
    public ClientResponse post(String url, String fileName, File file) throws IOException {
        HttpPost request = new HttpPost(url);
        FileBody fileBody = new FileBody(file);
        HttpEntity entity = MultipartEntityBuilder
                .create()
                .addPart(fileName, fileBody).build();

        request.setEntity(entity);
        return call(request);
    }

    @Override
    public ClientResponse delete(String url) throws IOException {
        return delete(url);
    }

    @Override
    public ClientResponse delete(String url, String body) throws IOException {
        return delete(url, body, FORM_CONTENT_TYPE);
    }

    @Override
    public ClientResponse delete(String url, String body, String contentType) throws IOException {
        HttpDeleteWithBody request = new HttpDeleteWithBody(url);
        request.setHeader(CONTENT_TYPE_HEADER, contentType);
        if (body != null) {
            request.setEntity(new StringEntity(body));
        }

        return call(request);
    }
}
