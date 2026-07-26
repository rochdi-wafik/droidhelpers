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
 * - Singleton: one shared OkHttpClient (connection pool, 15s connect /
 *   20s read timeouts) reused across the whole module.
 * ------------------------------------------------------------------------
 * - Headers are passed per-call (as a method parameter), NOT stored as
 *   mutable instance state. The old addHeaders()/clearHeaders() instance
 *   fields were removed because they raced across concurrent callers of
 *   the shared singleton (one request's headers could be cleared or
 *   overwritten by another in flight). Pass headers directly to each
 *   get()/post...() call instead.
 * ------------------------------------------------------------------------
 * - Requests can be tagged with a unique "requestId" to allow targeted
 *   cancellation later via cancelRequestById(requestId).
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
     * - onSuccess(rawData): fires for ANY HTTP response (2xx AND 4xx/5xx),
     *   the server mirrors the API status in the HTTP status and puts the
     *   error details in the body, so the caller (LicenseApiClient.parse())
     *   must read the body to extract error.code/error.message.
     * - onError(e): transport failures only (no connection, timeout, DNS).
     */
    public interface ICallback {
        void onSuccess(byte[] rawBytes, Headers headers);
        void onError(Exception e);
    }

    /**
     * ****************************************************************************
     *   Constructor (Private)
     * ****************************************************************************
     * - Use getInstance() to get singleton instance
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
     * ****************************************************************************
     * Get Instance
     * ****************************************************************************
     * - Use this to get singleton instance.
     * - This method is thread-safe using double-checked locking for lazy initialization.
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
     * ****************************************************************************
     *  Get HTTP Client
     * ****************************************************************************
     * - Retrieves the configured OkHttpClient instance for making HTTP requests.
     * ------------------------------------------------------------------------
     * @return The singleton OkHttpClient instance.
     * @throws IllegalStateException if the HttpClient has not been properly initialized
     * (e.g., if getInstance() hasn't been called, though it initializes lazily).
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
     * Apply Headers (private helper)
     * ************************************************************************
     * - Adds the given per-call headers (if any) to the request builder.
     * - Headers are never stored on the instance, so concurrent calls never
     *   interfere with each other's headers.
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
     * - GET Request with Callback.
     * - This method enqueues the request in Background Thread,
     *   You can safely call it from UI Thread.
     * - Be aware that ICallback is invoked in Background Thread.
     * ------------------------------------------------------------------------
     * @param url       Target endpoint.
     * @param ICallback Result ICallback.
     */
    public void get(String url, ICallback ICallback) {
        get(url, null, null, ICallback);
    }

    /**
     * ************************************************************************
     * get() with per-call headers
     * ************************************************************************
     * @param url       Target endpoint.
     * @param headers   Request headers for this call only (e.g. x-api-key), or null.
     * @param ICallback Result ICallback.
     */
    public void get(String url, @Nullable Map<String, String> headers, ICallback ICallback) {
        get(url, headers, null, ICallback);
    }

    /**
     * ************************************************************************
     * get() with per-call headers and request ID
     * ************************************************************************
     * @param url       Target endpoint.
     * @param headers   Request headers for this call only (e.g. x-api-key), or null.
     * @param requestId Unique ID to tag this request, allowing targeted cancellation later. Can be null.
     * @param ICallback Result ICallback.
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
     * ****************************************************************************
     *  Get (Async)
     * ****************************************************************************
     * - This method enqueues the request in Background Thread,
     *   You can safely call it from UI Thread.
     * - It returns Call object, you can use it for example to cancel the request
     * @param url      The URL to request.
     * @param callback The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call getAsync(String url, okhttp3.Callback callback) {
        return getAsync(url, null, null, callback);
    }

    /**
     * Get (Async) with per-call headers
     * @param url      The URL to request.
     * @param headers  Request headers for this call only, or null.
     * @param callback The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call getAsync(String url, @Nullable Map<String, String> headers, okhttp3.Callback callback) {
        return getAsync(url, headers, null, callback);
    }

    /**
     * Get (Async) with per-call headers and request ID
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag this request, allowing targeted cancellation later. Can be null.
     * @param callback  The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
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
     * ********************************************************************************
     *  Get (Sync)
     * ********************************************************************************
     * - Perform GET request synchronously. (blocking operation)
     * - Do not perform this method on UI Thread.
     * - You must handle background thread by yourself.
     * - This returns Response, which is used to read the response body and headers.
     * - You must close the Response body after reading it to avoid resource leaks.
     * ------------------------------------------------------------------------
     * @param url The URL to request.
     * @return Response Object.
     * @throws IOException if a network error occurs.
     */
    public Response getSync(String url) throws IOException {
        return getSync(url, null, null);
    }

    /**
     * Get (Sync) with per-call headers
     * @param url     The URL to request.
     * @param headers Request headers for this call only, or null.
     * @return Response Object.
     * @throws IOException if a network error occurs.
     */
    public Response getSync(String url, @Nullable Map<String, String> headers) throws IOException {
        return getSync(url, headers, null);
    }

    /**
     * Get (Sync) with per-call headers and request ID
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag this request, allowing targeted cancellation later. Can be null.
     * @return Response Object.
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
     * ****************************************************************************
     *  Post Json (Async)
     * ****************************************************************************
     * - Performs a POST request with a JSON body asynchronously (non-blocking).
     * - This method is executed on background thread.
     * - So you don't need to use background thread by yourself.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param jsonBody The JSON string to send as body.
     * @param callback The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call postJson(String url, String jsonBody, okhttp3.Callback callback) {
        return postJson(url, null, null, jsonBody, callback);
    }

    /**
     * Post Json (Async) with per-call headers
     * @param url      The URL to request.
     * @param headers  Request headers for this call only, or null.
     * @param jsonBody The JSON string to send as body.
     * @param callback The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call postJson(String url, @Nullable Map<String, String> headers, String jsonBody, okhttp3.Callback callback) {
        return postJson(url, headers, null, jsonBody, callback);
    }

    /**
     * Post Json (Async) with per-call headers and request ID
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag this request, allowing targeted cancellation later. Can be null.
     * @param jsonBody  The JSON string to send as body.
     * @param callback  The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
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
     * ********************************************************************************
     *  Post Json (Sync)
     * ********************************************************************************
     * - Perform Post request synchronously. (blocking operation)
     * - Warning: Do not perform this method on UI Thread.
     * - You must handle background thread by yourself.
     * ------------------------------------------------------------------------
     * @param url      The URL to request.
     * @param jsonBody The JSON string to send as body.
     * @return The OkHttp Response.
     * @throws IOException if a network error occurs.
     * @throws IllegalStateException if OkHttpClient is not initialized.
     */
    public Response postJsonSync(String url, String jsonBody) throws IOException {
        return postJsonSync(url, null, null, jsonBody);
    }

    /**
     * Post Json (Sync) with per-call headers
     * @param url      The URL to request.
     * @param headers  Request headers for this call only, or null.
     * @param jsonBody The JSON string to send as body.
     * @return The OkHttp Response.
     * @throws IOException if a network error occurs.
     */
    public Response postJsonSync(String url, @Nullable Map<String, String> headers, String jsonBody) throws IOException {
        return postJsonSync(url, headers, null, jsonBody);
    }

    /**
     * Post Json (Sync) with per-call headers and request ID
     * @param url       The URL to request.
     * @param headers   Request headers for this call only, or null.
     * @param requestId Unique ID to tag this request, allowing targeted cancellation later. Can be null.
     * @param jsonBody  The JSON string to send as body.
     * @return The OkHttp Response.
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
     * ***************************************************************************
     *  Cancel Request by ID
     * ***************************************************************************
     * - Cancels any currently running or queued HTTP request that was tagged
     *   with the specified requestId.
     * - This is useful for stopping a specific network activity (e.g., when
     *   an Activity/Fragment is destroyed, or a user cancels an action).
     * ------------------------------------------------------------------------
     * @param requestId The unique ID assigned to the request(s) to be canceled.
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
     * ***************************************************************************
     *  Cancel All Running Requests
     * ***************************************************************************
     * - Cancels all currently running and queued HTTP requests.
     * - This is useful for stopping all network activity, (i.e activity destroyed)
     * - Note: In-flight requests may still complete their network portion
     *  but their callbacks will not be executed
     *  if the thread executing them is interrupted.
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
     * ***************************************************************************
     *  Count Running Requests
     * ***************************************************************************
     * - Count any HTTP requests currently running.
     */
    public int countRunningRequests() {
        if (okHttpClient != null) {
            return okHttpClient.dispatcher().runningCallsCount();
        }
        return 0;
    }

    /**
     * ***************************************************************************
     *  Count Queued Requests
     * ***************************************************************************
     * - Count queued HTTP requests (waiting to be executed).
     */
    public int countQueuedRequests() {
        if (okHttpClient != null) {
            return okHttpClient.dispatcher().queuedCallsCount();
        }
        return 0;
    }

    /**
     * ***************************************************************************
     *  Count Pending Requests (running & queued)
     * ***************************************************************************
     * - Count pending HTTP requests (running and queued).
     */
    public int countPendingRequests() {
        if (okHttpClient != null) {
            return countRunningRequests() + countQueuedRequests();
        }
        return 0;
    }

    /**
     * ***********************************************************************
     *  Shutdown Client
     * ***********************************************************************
     * - Shuts down the OkHttpClient's internal thread pools and connection pool.
     * - This should typically only be called when the application process is ending
     * - or if you truly need to de-initialize the SDK completely and are sure
     * - no more network requests will be made.
     * - Calling this prematurely can lead to `RejectedExecutionException` for subsequent requests.
     * --
     * - For most ad SDKs, the HttpClient lives for the lifetime of the app.
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
     * - Shared executor for GET/POST: runs the call async and routes the
     *   response body (or transport error) into the ICallback.
     * ------------------------------------------------------------------------
     * @param request   Prepared OkHttp request.
     * @param ICallback Result ICallback.
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