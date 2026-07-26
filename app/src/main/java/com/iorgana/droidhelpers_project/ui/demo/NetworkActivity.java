package com.iorgana.droidhelpers_project.ui.demo;

import android.widget.EditText;
import android.widget.LinearLayout;

import com.iorgana.droidhelpers.network.AddressHelper;
import com.iorgana.droidhelpers.network.ConnectivityUtils;
import com.iorgana.droidhelpers.network.HttpClient;
import com.iorgana.droidhelpers.network.WifiHelper;
import com.iorgana.droidhelpers.stream.StreamUtils;
import com.iorgana.droidhelpers_project.app.App;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Headers;
import okhttp3.OkHttpClient;


/**
 * NetworkActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.network package:
 * HttpClient (OkHttp wrapper), AddressHelper (IP/domain/url utilities),
 * ConnectivityUtils (network state), WifiHelper (wifi state).
 */
public class NetworkActivity extends BaseDemoActivity {

    @Override
    protected String getScreenTitle() {
        return "Networking";
    }

    @Override
    protected void buildContent() {
        buildHttpClientSection();
        buildAddressHelperSection();
        buildConnectivitySection();
        buildWifiSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildHttpClientSection() {
        LinearLayout s = addSection("HttpClient",
                "Thin OkHttp singleton wrapper for GET/POST, sync or async, with per-request cancellation.");

        EditText urlInput = addInput(s, "GET/POST URL", "https://jsonplaceholder.typicode.com/todos/1");
        EditText jsonInput = addInput(s, "JSON body for POST", "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}");

        runSafe(addRow(s, "getInstance()"), () -> "singleton ready = " + (HttpClient.getInstance() != null));

        runSafe(addRow(s, "getInstance().getClient()  (raw OkHttpClient)"), () -> {
            OkHttpClient client =  HttpClient.getInstance().getClient();
            return "connectTimeout=" + client.connectTimeoutMillis() + "ms, readTimeout=" + client.readTimeoutMillis() + "ms";
        });

        Row getRow = addRow(s, "get(url, ICallback)  (async GET)");
        getRow.button.setOnClickListener(v -> {
            getRow.output.setText("Requesting...");
            HttpClient.getInstance().get(urlInput.getText().toString(), new HttpClient.ICallback() {
                @Override public void onSuccess(byte[] rawBytes, Headers headers) {
                    String body = StreamUtils.streamToString(new ByteArrayInputStream(rawBytes));
                    runOnUiThread(() -> getRow.output.setText(trim(body)));
                }
                @Override public void onError(Exception e) {
                    runOnUiThread(() -> getRow.output.setText("Error: " + e.getMessage()));
                }
            });
        });

        Row getSyncRow = addRow(s, "getSync(url)  (sync GET, run off the UI thread)");
        getSyncRow.button.setOnClickListener(v -> {
            getSyncRow.output.setText("Requesting...");
            new Thread(() -> {
                try (okhttp3.Response response = HttpClient.getInstance().getSync(urlInput.getText().toString())) {
                    String body = response.body() != null ? response.body().string() : "(empty body)";
                    runOnUiThread(() -> getSyncRow.output.setText("HTTP " + response.code() + " -> " + trim(body)));
                } catch (Exception e) {
                    runOnUiThread(() -> getSyncRow.output.setText("Error: " + e.getMessage()));
                }
            }).start();
        });

        Row postRow = addRow(s, "postJson(url, jsonBody, Callback)  (async POST)");
        postRow.button.setOnClickListener(v -> {
            postRow.output.setText("Posting...");
            HttpClient.getInstance().postJson(urlInput.getText().toString(), jsonInput.getText().toString(), new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException e) {
                    runOnUiThread(() -> postRow.output.setText("Error: " + e.getMessage()));
                }
                @Override public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    String body;
                    try { body = response.body() != null ? response.body().string() : "(empty)"; }
                    catch (Exception e) { body = "Error reading body: " + e.getMessage(); }
                    String finalBody = body;
                    runOnUiThread(() -> postRow.output.setText("HTTP " + response.code() + " -> " + trim(finalBody)));
                }
            });
        });

        Row postSyncRow = addRow(s, "postJsonSync(url, jsonBody)  (sync POST, run off the UI thread)");
        postSyncRow.button.setOnClickListener(v -> {
            postSyncRow.output.setText("Posting...");
            new Thread(() -> {
                try (okhttp3.Response response = HttpClient.getInstance().postJsonSync(urlInput.getText().toString(), jsonInput.getText().toString())) {
                    String body = response.body() != null ? response.body().string() : "(empty body)";
                    runOnUiThread(() -> postSyncRow.output.setText("HTTP " + response.code() + " -> " + trim(body)));
                } catch (Exception e) {
                    runOnUiThread(() -> postSyncRow.output.setText("Error: " + e.getMessage()));
                }
            }).start();
        });

        Row taggedRow = addRow(s, "get(url, headers, requestId, ICallback)  tagged \"demo-req-1\" (slow, to test cancel below)");
        taggedRow.button.setOnClickListener(v -> {
            taggedRow.output.setText("Requesting (5s delay)...");
            HttpClient.getInstance().get("https://httpbin.org/delay/5", null, "demo-req-1", new HttpClient.ICallback() {
                @Override public void onSuccess(byte[] rawBytes, Headers headers) {
                    runOnUiThread(() -> taggedRow.output.setText("Completed (not cancelled in time)"));
                }
                @Override public void onError(Exception e) {
                    runOnUiThread(() -> taggedRow.output.setText("Ended: " + e.getMessage()));
                }
            });
        });

        runSafe(addRow(s, "cancelRequestById(\"demo-req-1\")"), () -> {
            HttpClient.getInstance().cancelRequestById("demo-req-1");
            return "Cancel requested for tag: demo-req-1";
        });

        runSafe(addRow(s, "cancelAllRequests()"), () -> {
            HttpClient.getInstance().cancelAllRequests();
            return "All running/queued requests cancelled";
        });

        runSafe(addRow(s, "countRunningRequests() / countQueuedRequests() / countPendingRequests()"), () ->
                "running=" + HttpClient.getInstance().countRunningRequests()
                        + ", queued=" + HttpClient.getInstance().countQueuedRequests()
                        + ", pending=" + HttpClient.getInstance().countPendingRequests());

        runSafe(addRow(s, "shutdown()  (tears down + self-heals on next getInstance())"), () -> {
            HttpClient.getInstance().shutdown();
            return "Shut down. instance ready again = " + (HttpClient.getInstance() != null);
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildAddressHelperSection() {
        LinearLayout s = addSection("AddressHelper",
                "IP / domain / URL / CIDR validation and local network address helpers.");

        EditText ipInput = addInput(s, "IP to validate", "192.168.1.10");
        EditText domainInput = addInput(s, "Domain / URL to validate", "https://www.example.com:8080/path");
        EditText cidrInput = addInput(s, "CIDR to validate", "192.168.0.0/24");
        EditText dnsInput = addInput(s, "Host to ping-check (isIpReachable)", "8.8.8.8");

        runSafe(addRow(s, "getIPAddress(false)  (local IPv4)"), () -> AddressHelper.getIPAddress(false));
        runSafe(addRow(s, "getIPAddress(true)  (local IPv6)"), () -> AddressHelper.getIPAddress(true));

        runSafe(addRow(s, "getTakenIPs()  (all local interface IPs)"), () -> {
            List<String> taken = AddressHelper.getTakenIPs();
            return taken.isEmpty() ? "(none found)" : taken.toString();
        });

        runSafe(addRow(s, "generateRandomIP(\"192.168\", excludedIPs)"), () -> {
            List<String> excluded = new ArrayList<>(Collections.singletonList("192.168.1.1"));
            return AddressHelper.generateRandomIP("192.168", excluded);
        });

        runSafe(addRow(s, "isIp(ip)"), () -> String.valueOf(AddressHelper.isIp(ipInput.getText().toString())));
        runSafe(addRow(s, "isDomain(domain)"), () -> String.valueOf(AddressHelper.isDomain(domainInput.getText().toString())));
        runSafe(addRow(s, "isWebUrl(url)"), () -> String.valueOf(AddressHelper.isWebUrl(domainInput.getText().toString())));
        runSafe(addRow(s, "isValidCidr(cidr)"), () -> String.valueOf(AddressHelper.isValidCidr(cidrInput.getText().toString())));
        runSafe(addRow(s, "getDomain(url, withPort=true)"), () -> String.valueOf(AddressHelper.getDomain(domainInput.getText().toString(), true)));

        Row reachableRow = addRow(s, "isIpReachable(host, timeoutMs)  (blocking socket probe, run off UI thread)");
        reachableRow.button.setOnClickListener(v -> {
            reachableRow.output.setText("Checking...");
            new Thread(() -> {
                boolean reachable = AddressHelper.isIpReachable(dnsInput.getText().toString(), 3000);
                runOnUiThread(() -> reachableRow.output.setText(String.valueOf(reachable)));
            }).start();
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildConnectivitySection() {
        LinearLayout s = addSection("ConnectivityUtils",
                "Checks whether the device has an active network transport, and whether it actually has internet.");

        runSafe(addRow(s, "isConnected(context)"), () -> String.valueOf(ConnectivityUtils.isConnected(this)));
        runSafe(addRow(s, "hasInternet(context)"), () -> String.valueOf(ConnectivityUtils.hasInternet(this)));
    }

    /* ------------------------------------------------------------------ */
    private void buildWifiSection() {
        LinearLayout s = addSection("WifiHelper", "WiFi radio + hotspot state helpers.");

        runSafe(addRow(s, "isWifiEnabled(application)"), () -> String.valueOf(WifiHelper.isWifiEnabled(App.getContext())));
        runSafe(addRow(s, "setWifiEnabled(application, true)  (no-op on API 29+, restricted by Android)"), () -> {
            WifiHelper.setWifiEnabled(App.getContext(), true);
            return "Requested (Android 10+ blocks apps from toggling WiFi directly)";
        });
        runSafe(addRow(s, "isHotspotEnabled(application)"), () -> String.valueOf(WifiHelper.isHotspotEnabled(App.getContext())));
        runSafe(addRow(s, "getWifiIpAddress(context)"), () -> String.valueOf(WifiHelper.getWifiIpAddress(this)));
    }

    private static String trim(String s) {
        if (s == null) return "null";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
