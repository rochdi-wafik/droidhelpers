package com.iorgana.droidhelpers.network;

import android.util.Log;

import androidx.annotation.Nullable;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * ************************************************************************
 * HttpClient
 * ************************************************************************
 * - Thin OkHttp wrapper for basic network calls (GET/POST).
 * - Singleton with one shared OkHttpClient (connection pool, 15s connect
 *   / 20s read timeouts) reused across the whole module.
 * - Headers are passed per-call (method parameter), not stored as mutable
 *   instance state, preventing concurrency issues.
 * - Requests can be tagged with a unique "requestId" for targeted
 *   cancellation via cancelRequestById(requestId).
 * ------------------------------------------------------------------------
 * @apiNote The okhttp dependency must be declared as `api` (not
 *          `implementation`) in the build.gradle because OkHttpClient and
 *          related types appear in public method signatures.
 */
public class HttpClient {
    private static final String TAG = "__HttpClient";
    // singleton instances
    public static volatile HttpClient INSTANCE;
    private static volatile OkHttpClient okHttpClient;

    // Common Media Type for JSON requests
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * ************************************************************************
     * ICallback (Interface)
     * ************************************************************************
     * - Callback interface for HTTP request results.
     * - onSuccess: fires for ANY HTTP response (2xx, 4xx, 5xx).
     * - onError: fires for transport failures (no connection, timeout, DNS).
     */
    public interface ICallback {
        void onSuccess(byte[] rawBytes, Headers headers);
        void onError(Exception e);
    }

    /**
     * ************************************************************************
     * HttpClient (Private Constructor)
     * ************************************************************************
     * - Use getInstance() to get the singleton instance.
     */
    private HttpClient() {
        // Initialize OkHttpClient within the private constructor
        // This ensures it's only built once when the singleton instance is first created.
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS) // Connection timeout
                    .readTimeout(20, TimeUnit.SECONDS)    // Read timeout
                    .writeTimeout(20, TimeUnit.SECONDS)   // Write timeout
                    // Add other interceptors, caches, or configurations here if needed globally
                    // .addInterceptor(new LoggingInterceptor()) // Example: For request/response logging
                    .build();
            Logger.d(TAG + " HttpClient(): OkHttpClient instance created and configured");
        }
    }

    /**
     * ************************************************************************
     * getInstance()
     * ************************************************************************
     * - Get the singleton HttpClient instance (thread-safe, lazy init).
     * ------------------------------------------------------------------------
     * @return The singleton HttpClient instance.
     */
    public static HttpClient getInstance() {
        if (INSTANCE == null) { // First check (no lock)
            synchronized (HttpClient.class) {
                if (INSTANCE == null) {  // Second check (within lock)
                    INSTANCE = new HttpClient();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * ************************************************************************
     * getClient()
     * ************************************************************************
     * - Get the configured OkHttpClient instance.
     * ------------------------------------------------------------------------
     * @return The singleton OkHttpClient instance.
     * @throws IllegalStateException if HttpClient is not initialized.
     * @apiNote The okhttp dependency must be declared as `api` (not
     *          `implementation`) in the build.gradle because OkHttpClient and
     *          related types appear in public method signatures.
     */
    public OkHttpClient getClient() {
        if (okHttpClient == null) {
            // This case should ideally not be reached if getInstance() is called first,
            // but provides a fail-safe.
            throw new IllegalStateException("OkHttpClient has not been initialized. Call HttpClient.getInstance() first.");
        }
        return okHttpClient;
    }

    /**
     * ************************************************************************
     * applyHeaders() (Private Helper)
     * ************************************************************************
     * - Add per-call headers to the request builder.
     * - Headers are never stored on the instance to prevent concurrency issues.
     * ------------------------------------------------------------------------
     * @param builder The request builder to add headers to.
     * @param headers The headers map, or null.
     */
    private static void applyHeaders(Request.Builder builder, @Nullable Map<String, String> headers) {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }

    /////////////////////////// GET Methods ////////////////////////////////////

    /**
     * ************************************************************************
     * get()
     * ************************************************************************
     * - Perform a GET request asynchronously with callback.
     * - Safe to call from UI Thread (enqueued in background thread).
     * - ICallback is invoked in the background thread.
     * ------------------------------------------------------------------------
     * @param url       Target endpoint.
     * @param ICallback Result callback.
     */
    public void get(String url, ICallback ICallback) {
        get(url, null, null, ICallback);
    }

    /**
     * ************************************************************************
     * get() with per-call headers
     * ************************************************************************
     * - Perform a GET request with custom headers.
     * ------------------------------------------------------------------------
     * @param url       Target endpoint.
     * @param headers   Request headers for this call only (e.g., x-api-key), or null.
     * @param ICallback Result callback.
     */
    public void get(String url, @Nullable Map<String, String> headers, ICallback ICallback) {
        get(url, headers, null, ICallback);
    }

    /**
     * ************************************************************************
     * get() with per-call headers and request ID
     * ************************************************************************
     * - Perform a GET request with custom headers and request ID.
     * ------------------------------------------------------------------------
     * @param url       Target endpoint.
     * @param headers   Request headers for this call only (e.g., x-api-key), or null.
     * @param requestId Unique ID to tag this request for targeted cancellation. Can be null.
     * @param ICallback Result callback.
     */
    public void get(String url, @Nullable Map<String, String> headers, @Nullable String requestId, ICallback ICallback) {
        Request.Builder builder = new Request.Builder().url(url);
        if (requestId != null) {
            builder.tag(requestId);
        }
        applyHeaders(builder, headers);
        Request request = builder.get().build();
        // enqueue in background
        this.enqueue(request, ICallback);
    }

    /**
     * ************************************************************************
     * getAsync()
     * ************************************************************************
     * - Perform a GET request asynchronously with an OkHttp Callback.
     * - Safe to call from UI Thread. Returns a Call object for cancellation.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param callback The OkHttp Callback to handle the response.
     * @return The OkHttp Call object (can be used to cancel this request).
     */
    public Call getAsync(String url, okhttp3.Callback callback) {
        return getAsync(url, null, null, callback);
    }

    /**
     * ************************************************************************
     * getAsync() with per-call headers
     * ************************************************************************
     * - Perform a GET request asynchronously with custom headers.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param headers  Request headers for this call only, or null.
     * @param callback The OkHttp Callback to handle the response.
     * @return The OkHttp Call object (can be used to cancel this request).
     */
    public Call getAsync(String url, @Nullable Map<String, String> headers, okhttp3.Callback callback) {
        return getAsync(url, headers, null, callback);
    }

    /**
     * ************************************************************************
     * getAsync() with per-call headers and request ID
     * ************************************************************************
     * - Perform a GET request asynchronously with custom headers and request ID.
     * ------------------------------------------------------------------------
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag for targeted cancellation. Can be null.
     * @param callback  The OkHttp Callback to handle the response.
     * @return The OkHttp Call object (can be used to cancel this request).
     */
    public Call getAsync(String url, @Nullable Map<String, String> headers, @Nullable String requestId, okhttp3.Callback callback) {
        OkHttpClient client = getClient(); // Throws if not initialized
        Request.Builder builder = new Request.Builder().url(url);
        if (requestId != null) {
            builder.tag(requestId);
        }
        applyHeaders(builder, headers);
        Request request = builder.get().build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call; // Return the Call object
    }

    /**
     * ************************************************************************
     * getSync()
     * ************************************************************************
     * - Perform a GET request synchronously (blocking).
     * - Do NOT call this on the UI Thread. You must handle threading yourself.
     * ------------------------------------------------------------------------
     * @param url The URL to request.
     * @return The OkHttp Response object.
     * @throws IOException if a network error occurs.
     */
    public Response getSync(String url) throws IOException {
        return getSync(url, null, null);
    }

    /**
     * ************************************************************************
     * getSync() with per-call headers
     * ************************************************************************
     * - Perform a synchronous GET request with custom headers.
     * ------------------------------------------------------------------------
     * @param url     The URL to request.
     * @param headers Request headers for this call only, or null.
     * @return The OkHttp Response object.
     * @throws IOException if a network error occurs.
     */
    public Response getSync(String url, @Nullable Map<String, String> headers) throws IOException {
        return getSync(url, headers, null);
    }

    /**
     * ************************************************************************
     * getSync() with per-call headers and request ID
     * ************************************************************************
     * - Perform a synchronous GET request with custom headers and request ID.
     * ------------------------------------------------------------------------
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag for targeted cancellation. Can be null.
     * @return The OkHttp Response object.
     * @throws IOException if a network error occurs.
     */
    public Response getSync(String url, @Nullable Map<String, String> headers, @Nullable String requestId) throws IOException {
        OkHttpClient client = getClient();
        Request.Builder builder = new Request.Builder().url(url);
        if (requestId != null) {
            builder.tag(requestId);
        }
        applyHeaders(builder, headers);
        Request request = builder.get().build();
        return client.newCall(request).execute();
    }

    /////////////////////////// POST Methods ////////////////////////////////////

    /**
     * ************************************************************************
     * postJson()
     * ************************************************************************
     * - Perform a POST request with a JSON body asynchronously.
     * - The request is executed on a background thread.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param jsonBody The JSON string to send as the request body.
     * @param callback The OkHttp Callback to handle the response.
     * @return The OkHttp Call object (can be used to cancel this request).
     */
    public Call postJson(String url, String jsonBody, okhttp3.Callback callback) {
        return postJson(url, null, null, jsonBody, callback);
    }

    /**
     * ************************************************************************
     * postJson() with per-call headers
     * ************************************************************************
     * - Perform a POST request with JSON body and custom headers.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param headers  Request headers for this call only, or null.
     * @param jsonBody The JSON string to send as the request body.
     * @param callback The OkHttp Callback to handle the response.
     * @return The OkHttp Call object (can be used to cancel this request).
     */
    public Call postJson(String url, @Nullable Map<String, String> headers, String jsonBody, okhttp3.Callback callback) {
        return postJson(url, headers, null, jsonBody, callback);
    }

    /**
     * ************************************************************************
     * postJson() with per-call headers and request ID
     * ************************************************************************
     * - Perform a POST request with JSON body, custom headers, and request ID.
     * ------------------------------------------------------------------------
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag for targeted cancellation. Can be null.
     * @param jsonBody  The JSON string to send as the request body.
     * @param callback  The OkHttp Callback to handle the response.
     * @return The OkHttp Call object (can be used to cancel this request).
     */
    public Call postJson(String url, @Nullable Map<String, String> headers, @Nullable String requestId, String jsonBody, okhttp3.Callback callback) {
        OkHttpClient client = getClient(); // Throws if not initialized
        RequestBody body = okhttp3.RequestBody.create(jsonBody, MEDIA_TYPE_JSON);
        Request.Builder builder = new Request.Builder().url(url);
        if (requestId != null) {
            builder.tag(requestId);
        }
        applyHeaders(builder, headers);

        Request request = builder.post(body).build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call; // Return the Call object
    }

    /**
     * ************************************************************************
     * postJsonSync()
     * ************************************************************************
     * - Perform a POST request with a JSON body synchronously (blocking).
     * - Do NOT call this on the UI Thread.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param jsonBody The JSON string to send as the request body.
     * @return The OkHttp Response object.
     * @throws IOException if a network error occurs.
     */
    public Response postJsonSync(String url, String jsonBody) throws IOException {
        return postJsonSync(url, null, null, jsonBody);
    }

    /**
     * ************************************************************************
     * postJsonSync() with per-call headers
     * ************************************************************************
     * - Perform a synchronous POST request with JSON body and custom headers.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param headers  Request headers for this call only, or null.
     * @param jsonBody The JSON string to send as the request body.
     * @return The OkHttp Response object.
     * @throws IOException if a network error occurs.
     */
    public Response postJsonSync(String url, @Nullable Map<String, String> headers, String jsonBody) throws IOException {
        return postJsonSync(url, headers, null, jsonBody);
    }

    /**
     * ************************************************************************
     * postJsonSync() with per-call headers and request ID
     * ************************************************************************
     * - Perform a synchronous POST request with JSON body, custom headers,
     *   and request ID.
     * ------------------------------------------------------------------------
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag for targeted cancellation. Can be null.
     * @param jsonBody  The JSON string to send as the request body.
     * @return The OkHttp Response object.
     * @throws IOException if a network error occurs.
     */
    public Response postJsonSync(String url, @Nullable Map<String, String> headers, @Nullable String requestId, String jsonBody) throws IOException {
        OkHttpClient client = getClient();
        RequestBody body = okhttp3.RequestBody.create(jsonBody, MEDIA_TYPE_JSON);
        Request.Builder builder = new Request.Builder().url(url);
        if (requestId != null) {
            builder.tag(requestId);
        }
        applyHeaders(builder, headers);
        Request request = builder.post(body).build();
        return client.newCall(request).execute();
    }

    ///////////////////////////// Utility Methods //////////////////////////////////

    /**
     * ************************************************************************
     * cancelRequestById()
     * ************************************************************************
     * - Cancel any running or queued HTTP request tagged with the specified ID.
     * ------------------------------------------------------------------------
     * @param requestId The unique ID of the request(s) to cancel.
     */
    public void cancelRequestById(@Nullable String requestId) {
        if (okHttpClient == null || requestId == null) {
            return;
        }

        int canceledCount = 0;

        // Cancel queued calls
        for (Call call : okHttpClient.dispatcher().queuedCalls()) {
            if (requestId.equals(call.request().tag())) {
                call.cancel();
                canceledCount++;
            }
        }

        // Cancel running calls
        for (Call call : okHttpClient.dispatcher().runningCalls()) {
            if (requestId.equals(call.request().tag())) {
                call.cancel();
                canceledCount++;
            }
        }

        if (canceledCount > 0) {
            Logger.d(TAG + " cancelRequestById(): Canceled " + canceledCount + " request(s) with ID: " + requestId);
        }
    }

    /**
     * ************************************************************************
     * cancelAllRequests()
     * ************************************************************************
     * - Cancel all running and queued HTTP requests.
     * - Useful when an Activity is destroyed.
     */
    public void cancelAllRequests() {
        if (okHttpClient != null) {
            okHttpClient.dispatcher().cancelAll();
            Logger.d(TAG + " cancelAllRequests(): All HTTP requests cancelled.");
        } else {
            Logger.w(TAG + " cancelAllRequests(): OkHttpClient not initialized, cannot cancel requests.");
        }
    }

    /**
     * ************************************************************************
     * countRunningRequests()
     * ************************************************************************
     * - Count the number of HTTP requests currently running.
     * ------------------------------------------------------------------------
     * @return The number of running requests.
     */
    public int countRunningRequests() {
        if (okHttpClient != null) {
            return okHttpClient.dispatcher().runningCallsCount();
        }
        return 0;
    }

    /**
     * ************************************************************************
     * countQueuedRequests()
     * ************************************************************************
     * - Count the number of queued HTTP requests (waiting to be executed).
     * ------------------------------------------------------------------------
     * @return The number of queued requests.
     */
    public int countQueuedRequests() {
        if (okHttpClient != null) {
            return okHttpClient.dispatcher().queuedCallsCount();
        }
        return 0;
    }

    /**
     * ************************************************************************
     * countPendingRequests()
     * ************************************************************************
     * - Count the total pending HTTP requests (running + queued).
     * ------------------------------------------------------------------------
     * @return The number of pending requests.
     */
    public int countPendingRequests() {
        if (okHttpClient != null) {
            return countRunningRequests() + countQueuedRequests();
        }
        return 0;
    }

    /**
     * ************************************************************************
     * shutdown()
     * ************************************************************************
     * - Shut down the OkHttpClient's thread pools and connection pool.
     * - Should only be called when the application process is ending.
     * - Calling this prematurely may cause RejectedExecutionException for
     *   subsequent requests.
     * ------------------------------------------------------------------------
     * @apiNote For most SDKs, the HttpClient lives for the lifetime of the app.
     */
    public void shutdown() {
        if (okHttpClient != null) {
            Log.d(TAG, "shutdown: Shutting down OkHttpClient and its resources.");
            okHttpClient.dispatcher().executorService().shutdown();
            okHttpClient.connectionPool().evictAll();
            okHttpClient = null; // Clear the reference
            INSTANCE = null; // Clear the singleton instance reference
        } else {
            Logger.w(TAG + " shutdown(): OkHttpClient not initialized, nothing to shut down.");
        }
    }

    /**
     * ************************************************************************
     * enqueue()
     * ************************************************************************
     * - Execute an HTTP request asynchronously and route the response to the
     *   provided ICallback.
     * ------------------------------------------------------------------------
     * @param request   The prepared OkHttp request.
     * @param ICallback The callback to handle the response or error.
     */
    private void enqueue(Request request, ICallback ICallback) {
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            // onFailure() Called when the request failed at the transport level
            // (e.g. no network connectivity, DNS failure, timeout)
            // before any response was received
            @Override public void onFailure(Call call, IOException e) {
                ICallback.onError(e);
            }
            //  onResponse() Called when the server responded,
            //  regardless of HTTP status code success or not
            @Override public void onResponse(Call call, Response response) {
                // if response contains body, means HTTP success
                try (ResponseBody body = response.body()) {
                    byte[] rawBytes = body.bytes();
                    ICallback.onSuccess(rawBytes, response.headers());
                } catch (Exception e) {
                    // HTTP failure
                    ICallback.onError(e);
                }
            }
        });
    }
}