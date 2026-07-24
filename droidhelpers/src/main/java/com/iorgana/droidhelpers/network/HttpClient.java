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
 * @// TODO: 7/24/2026 Maybe we need to add "ID" for each request,
 *           So that we can access that request later i.e Close request by ID
 */
public class HttpClient {
    private static final String TAG = "__HttpClient";
    // singleton instances
    public static volatile HttpClient INSTANCE;
    private static volatile OkHttpClient okHttpClient;

    // Common Media Type for JSON requests
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    // Headers
    @Nullable Map<String, String> headers = null;
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
            Logger.d(TAG+" HttpClient(): OkHttpClient instance created and configured");
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
        // Clear headers before use the client
        INSTANCE.clearHeaders();
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
     * Add Headers
     * ************************************************************************
     * @param headers request headers like x-api-key
     */
    public void addHeaders(@Nullable Map<String, String> headers){
        this.headers = headers;
    }

    /**
     * ************************************************************************
     * Clear Headers
     * ************************************************************************
     * Make sure to clear old headers before using new http call.
     */
    public void clearHeaders(){
        this.headers = null;
    }
    /////////////////////////// GET Methods ////////////////////////////////////
    /**
     * ************************************************************************
     * get()
     * ************************************************************************
     * - GET Request with Callback.
     * - This method enqueue the request in Background Thread,
     *   You can safely call it from UI Thread.
     * - Be aware that ICallback invoked in Background Thread.
     * ------------------------------------------------------------------------
     * @param url    Target endpoint.
     * @param ICallback     Result ICallback.
     */
    public void get(String url, ICallback ICallback) {
        Request.Builder builder = new Request.Builder().url(url);
        // Add any headers (i.e. x-api-key)
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        // pass the request to enqueu
        Request request = builder.get().build();
        // enqueue in background
        this.enqueue(request, ICallback);
    }

    /**
     * ****************************************************************************
     *  Get (Async)
     * ****************************************************************************
     * - This method enqueue the request in Background Thread,
     *   You can safely call it from UI Thread.
     * - It returns Call object, you can use it for example to cancel the request
     * @param url The URL to request.
     * @param callback The OkHttp ICallback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     * // TODO: 7/24/2026 Maybe thisgetAsync()  method confuse with the above get() method?
     */
    public Call getAsync(String url, okhttp3.Callback callback) {
        OkHttpClient client = getClient(); // Throws if not initialized
        Request.Builder builder = new okhttp3.Request.Builder().url(url);
        // Add any headers (i.e. x-api-key)
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        okhttp3.Request request = builder.get().build();
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
     * - This returns Response, which used to read the response body and headers.
     * - You must close the Response body after reading it to avoid resource leaks.
     * ------------------------------------------------------------------------
     * @param url The URL to request.
     * @return Response Object.
     * @throws IOException if a network error occurs.
     */
    public Response getSync(String url) throws IOException {
        OkHttpClient client = getClient();
        Request.Builder builder = new Request.Builder().url(url);
        // Add any headers (i.e. x-api-key)
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.get().build();
        return client.newCall(request).execute();
    }

    /////////////////////////// POST Methods ////////////////////////////////////

    /**
     * ****************************************************************************
     *  Post Json (Async)
     * ****************************************************************************
     * - Performs a POST request with a JSON body asynchronously (none-blocking).
     * - This method is executed on background thread.
     * - So you don't need to use background thread by yourself.
     * ------------------------------------------------------------------------
     * @param url The URL to request.
     * @param jsonBody The JSON string to send as body.
     * @param callback The OkHttp ICallback to handle response.
     * @return The OkHttp Call object, which can be used to cancel this specific call.
     */
    public Call postJson(String url, String jsonBody, okhttp3.Callback callback) {
        OkHttpClient client = getClient(); // Throws if not initialized
        okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonBody, MEDIA_TYPE_JSON);
        Request.Builder builder = new okhttp3.Request.Builder().url(url);
        // Add any headers (i.e. x-api-key)
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

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
     * @param jsonBody The JSON string to send as body.
     * @return The OkHttp Response.
     * @throws IOException if a network error occurs.
     * @throws IllegalStateException if OkHttpClient is not initialized.
     */
    public Response postJsonSync(String url, String jsonBody) throws IOException {
        OkHttpClient client = getClient();
        RequestBody body = okhttp3.RequestBody.create(jsonBody, MEDIA_TYPE_JSON);
        Request.Builder builder = new okhttp3.Request.Builder().url(url);
        // Add any headers (i.e. x-api-key)
        if (this.headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Request request = builder.post(body).build();
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
        clearHeaders();
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
        clearHeaders();
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

    /**
     * ************************************************************************
     * enqueue()
     * ************************************************************************
     * - Shared executor for GET/POST: runs the call async and routes the
     *   response body (or transport error) into the ICallback.
     * ------------------------------------------------------------------------
     * @param request Prepared OkHttp request.
     * @param ICallback      Result ICallback.
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