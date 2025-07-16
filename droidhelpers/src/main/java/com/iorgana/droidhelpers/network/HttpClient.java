package com.iorgana.droidhelpers.network;


import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClient {
    private static final String TAG = "__HttpClient";
    // singleton instances
    public static volatile HttpClient INSTANCE;
    private static volatile OkHttpClient okHttpClient;
    // Common Media Type for JSON requests
    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");



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
                    .connectTimeout(10, TimeUnit.SECONDS) // Connection timeout
                    .readTimeout(10, TimeUnit.SECONDS)    // Read timeout
                    .writeTimeout(10, TimeUnit.SECONDS)   // Write timeout
                    // Add other interceptors, caches, or configurations here if needed globally
                    // .addInterceptor(new LoggingInterceptor()) // Example: For request/response logging
                    .build();
            Logger.d(TAG+" HttpClient(): OkHttpClient instance created and configured");
        }
    }

    /**
     * ****************************************************************************
     * Get Instance
     * ****************************************************************************
     * - Use this to get singleton singleton instance.
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
     *  Get Client
     * ****************************************************************************
     * - Retrieves the configured OkHttpClient instance for making HTTP requests.
     *
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

    /////////////////////////// RestFul (Async) ////////////////////////////////////
    /**
     * ****************************************************************************
     *  Get (Async)
     * ****************************************************************************
     * - Performs a GET request asynchronously (none-blocking).
     * - This method is executed on background thread.
     * - So you don't need to use background thread by yourself.
     * @param url The URL to request.
     * @param callback The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call get(String url, Callback callback) {
        OkHttpClient client = getClient(); // Throws if not initialized
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .get()
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call; // Return the Call object
    }

    /**
     * ****************************************************************************
     *  Post Json (Async)
     * ****************************************************************************
     * Performs a POST request with a JSON body asynchronously (none-blocking).
     * - This method is executed on background thread.
     * - So you don't need to use background thread by yourself.
     * @param url The URL to request.
     * @param jsonBody The JSON string to send as body.
     * @param callback The OkHttp Callback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call postJson(String url, String jsonBody, Callback callback) {
        OkHttpClient client = getClient(); // Throws if not initialized
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call; // Return the Call object
    }

    /////////////////////////// RestFul (Sync) ////////////////////////////////////

    /**
     * ********************************************************************************
     *  Get (Sync)
     * ********************************************************************************
     * - Perform GET request synchronously. (blocking operation)
     * - Do not perform this method on UI Thread.
     * - You must handle background thread by yourself.
     * @param url The URL to request.
     * @return The OkHttp Response.
     * @throws IOException if a network error occurs.
     * @throws IllegalStateException if OkHttpClient is not initialized.
     */
    public Response getSync(String url) throws IOException {
        OkHttpClient client = getClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return client.newCall(request).execute();
    }

    /**
     * ********************************************************************************
     *  Post Json (Sync)
     * ********************************************************************************
     * - Perform Post request synchronously. (blocking operation)
     * - Do not perform this method on UI Thread.
     * - You must handle background thread by yourself.
     * @param jsonBody The JSON string to send as body.
     * @return The OkHttp Response.
     * @throws IOException if a network error occurs.
     * @throws IllegalStateException if OkHttpClient is not initialized.
     */
    public Response postJsonSync(String url, String jsonBody) throws IOException {
        OkHttpClient client = getClient();
        RequestBody body = okhttp3.RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        return client.newCall(request).execute();
    }

    ///////////////////////////// Utility Methods //////////////////////////////////

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
            Logger.d(TAG+" cancelAllRequests(): All HTTP requests cancelled.");
        } else {
            Logger.w(TAG+" cancelAllRequests(): OkHttpClient not initialized, cannot cancel requests.");
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
            Logger.w(TAG+" shutdown(): OkHttpClient not initialized, nothing to shut down.");
        }
    }
}