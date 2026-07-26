# 🛠️ DroidHelpers - Usage Examples

Quick-start examples for all helpers in the library.

> **Gradle**: `implementation 'com.github.rochdi-wafik:droidhelpers:x.x.x'`  
> **Min SDK**: 28 | **Language**: Java (Kotlin interop ready)

---

## 📦 Database

### SqlPreferences - SQLite + Cache + Encryption

```java
// Application.java - call once at startup
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // ----- Init (background)
        // with custom secret key
        SqlPreferences.init(this, "MY_KEY_HERE", () -> Log.d("SqlPrefs", "Cache ready"));
        // without custom secret key
        SqlPreferences.init(this, () -> Log.d("SqlPrefs", "Cache ready"));
        
        // -------- Init Sync (Load To Cache Memory)
        // With Custom Secret Key
        SqlPreferences.getInstance(activity, "MY_KEY_HERE").initSync();
        // Without Custom Secret Key
        SqlPreferences.getInstance(activity).initSync();
        
        
    }
}
/**
 * Set Secret Key
 * 
 * - Secret Key must be 16, 24, or 32 bytes long (128, 192, or 256 bits).
 * - If an invalid key is provided in debug mode, it throws an exception.
 * - In production, it silently falls back to the DEFAULT_SECRET_KEY to prevent crashes.
 * 
 * Example: String secretKey = "Fin7@3h6K#t5?f&9";
 */

// Save (chain + apply)
SqlPreferences.getInstance(context)
    .putString("name", "Sami")
    .putInt("age", 28)
    .putBoolean("admin", true)
    .putDouble("score", 99.5)
    .putObject("profile", new User("Sami", "sami@mail.com"))
    .putListObject("friends", Arrays.asList(user1, user2))
    .apply();  // Required to persist!

// Read (instant, from memory cache)
String name = SqlPreferences.getInstance(context).getString("name", "Guest");
int age = SqlPreferences.getInstance(context).getInt("age", 0);
User me = SqlPreferences.getInstance(context).getObject("profile", User.class);
List<User> friends = SqlPreferences.getInstance(context).getListObject("friends", User.class);

// Remove / Clear
SqlPreferences.getInstance(context).remove("name");
SqlPreferences.getInstance(context).removeObject("profile");
SqlPreferences.getInstance(context).clear();

// Security (set BEFORE init)
SqlPreferences.SECRET_KEY = "Your32ByteSecretKeyHere123456"; // 16/24/32 bytes
SqlPreferences.ENABLE_ENCRYPTION = false; // Debug only
```

### SimpleDB - Encrypted SharedPreferences (Objects)

```java
// Save
SimpleDB.getInstance(context).saveObject("user", user);
SimpleDB.getInstance(context).saveListObject("users", userList);

// Read
User u = SimpleDB.getInstance(context).getObject("user", User.class);
List<User> list = SimpleDB.getInstance(context).getListObject("users", User.class);

// Config
SimpleDB.enableEncryption = false; // Before first use
SimpleDB.getInstance(context).setAllowSaveNull(true);
```

### SecurePreferences - Direct Encrypted SharedPreferences

```java
SharedPreferences prefs = SecurePreferences.getSharedPreferences(context, "auth_prefs");
prefs.edit().putString("token", "abc123").apply();
String token = prefs.getString("token", "");
```

---

## 🌐 Network

### HttpClient - OkHttp Wrapper (Singleton)

```java
HttpClient client = HttpClient.getInstance();

// Common headers (API key, auth)
Map<String, String> headers = new HashMap<>();
headers.put("Authorization", "Bearer " + token);
headers.put("X-API-Key", "key");

// Async GET with callback
client.get("https://api.example.com/users", headers, new HttpClient.ICallback() {
    @Override public void onSuccess(byte[] data, Headers headers) {
        String json = new String(data, StandardCharsets.UTF_8);
        runOnUiThread(() -> updateUI(json));
    }
    @Override public void onError(Exception e) {
        Log.e("API", e.getMessage());
    }
});

// Async GET with OkHttp Callback (cancellable)
Call call = client.getAsync("https://api.example.com/data", new Callback() {
    @Override public void onFailure(Call c, IOException e) {}
    @Override public void onResponse(Call c, Response r) {}
});
// call.cancel();

// Sync GET (background thread only!)
try (Response response = client.getSync("https://api.example.com/data")) {
    String body = response.body().string();
}

// POST JSON
client.postJson("https://api.example.com/users", "{\"name\":\"Sami\"}", new Callback() {...});
// Sync POST
try (Response r = client.postJsonSync(url, json)) { ... }

// Request tracking
int running = client.countRunningRequests();
int queued = client.countQueuedRequests();
client.cancelAllRequests();
client.shutdown(); // App shutdown
```

### ConnectivityUtils - Network State

```java
// Any network (cellular, wifi, vpn, ethernet)
boolean connected = ConnectivityUtils.isConnected(context);

// Validated internet access
boolean online = ConnectivityUtils.hasInternet(context);
```

### AddressHelper - IP / Host Utilities

```java
// Validate
boolean validIp = AddressHelper.isValidIp("192.168.1.1");
boolean validCidr = AddressHelper.isValidCidr("10.0.0.0/8");
boolean validHost = AddressHelper.isValidHostname("example.com");

// Local IPs
List<String> ips = AddressHelper.getLocalIpAddresses();

// Port check (async)
AddressHelper.isPortOpen("192.168.1.1", 80, 3000, open -> {
    Log.d("Port 80", open ? "open" : "closed");
});

// DNS resolve
AddressHelper.resolveHostname("google.com", ips -> {...});
```

### WifiHelper - WiFi State

```java
// Requires Application context
boolean wifiOn = WifiHelper.isWifiEnabled(app);
WifiHelper.setWifiEnabled(app, true); // Deprecated API 29+
Boolean hotspotOn = WifiHelper.isHotspotEnabled(app); // May return null
```

---

## 🔐 Crypto

### CryptoUtil - AES-GCM Encryption

```java
String secret = "MySecretKey12345"; // Any length, hashed to 256-bit

// String API (Base64 output)
String encrypted = CryptoUtil.cipherEncrypt("Hello World", secret);
String decrypted = CryptoUtil.cipherDecrypt(encrypted, secret);

// Byte array API
byte[] ct = CryptoUtil.cipherEncrypt("data".getBytes(StandardCharsets.UTF_8), secret);
byte[] pt = CryptoUtil.cipherDecrypt(ct, secret);

// RSA Signature Verification
boolean valid = CryptoUtil.verifyRSASignature(
    payloadBytes, signatureBase64, publicKeyBase64
);

// Android Keystore Master Key (for EncryptedSharedPreferences)
String alias = CryptoUtil.getOrCreateMasterKeyAlias();
```

### HmacVerifier - HMAC-SHA256 (Constant-Time)

```java
// Verify API response integrity
boolean ok = HmacVerifier.verify(payloadJson, signatureHex, sharedSecret);
```

### Base64Helper - URL-Safe Decode

```java
String json = Base64Helper.base64UrlDecode(base64UrlEncodedString);
```

### XOR Obfuscation (NOT secure - obfuscation only)

```java
String obf = CryptoUtil.xorEncrypt("data", "key");
String plain = CryptoUtil.xorDecrypt(obf, "key");
// xorEncryptDecrypt() works both ways
```

---

## 🔔 Notifications

### NotificationMaker - Builder API

```java
// One-off notification
NotificationMaker nm = new NotificationMaker(context)
    .setTitle("Download Complete")
    .setContent("File saved to Downloads")
    .setResIcon(R.drawable.ic_done)
    .setActivityPendingIntent(pendingIntent)
    .setAction(R.drawable.ic_open, "Open", actionIntent)
    .setOnGoing(false)
    .setAlertOnce(true);
nm.show();

// Shared notification (foreground service)
NotificationMaker shared = NotificationMaker.getInstance(context)
    .setTitle("Uploading...")
    .setContent("50%")
    .setOnGoing(true)
    .setChannelID("upload_channel");
shared.show();

// Update in-place
shared.updateNotification("Uploading...", "75%", R.drawable.ic_upload);

// Cancel
shared.cancelNotification();

// Permission check (Android 13+)
if (nm.areNotificationsEnabled()) { ... }
```

---

## 🪟 Alerts & UI

### AlertMaker - Bootstrap-Style Inline Alerts

```java
// Simple alert (returns LinearLayout to add to your view)
LinearLayout alert = new AlertMaker(context)
    .setType(AlertMaker.AlertType.Success)   // Primary, Secondary, Info, Success, Warning, Danger, Default, HTML
    .setTitle("Saved")
    .setContent("Your changes have been saved.")
    .setCancelable(true)
    .setListener(() -> Log.d("Alert", "Dismissed"))
    .build();

// Add to your layout
container.addView(alert);

// HTML Alert
new AlertMaker(context)
    .setType(AlertMaker.AlertType.HTML)
    .setHtml("<b>Bold</b> <i>italic</i> <a href='https://...'>link</a>")
    .build();
```

**Alert Types**: `Primary`, `Secondary`, `Info`, `Success`, `Warning`, `Danger`, `DEFAULT`, `HTML`

---

## ⏱️ Timers

### CountdownTimer - Pause/Resume/Delay

```java
CountdownTimer timer = new CountdownTimer(60); // 60 seconds

timer.setListener(new CountdownTimer.TimerListener() {
    @Override public void onChange(int secondsLeft) {
        runOnUiThread(() -> tv.setText(secondsLeft + "s"));
    }
    @Override public void onStateChanged(CountdownTimer.State state) {
        // RUNNING, PAUSED, STOPPED, COMPLETED
    }
    @Override public void onComplete() {
        runOnUiThread(() -> toast("Time's up!"));
    }
});

timer.start();
timer.pause();
timer.resume();
timer.delay(10);   // Add 10 seconds
timer.end();       // Stop & mark STOPPED
```

### ChronometerTimer - Stopwatch

```java
ChronometerTimer chrono = new ChronometerTimer();
chrono.start();
long elapsedMs = chrono.getElapsedTime();
chrono.stop();
chrono.reset();
```

---

## 🔄 Converters

### DataSizeConverter - Bytes ↔ Human Readable

```java
// Format bytes
String size = DataSizeConverter.byteToString(1536L);      // "1.5 KB"
String size2 = DataSizeConverter.byteToString(2_500_000); // "2.4 MB"

// Convert units
double mb = DataSizeConverter.bytesToMegabytes(5_000_000L); // 4.77
long bytes = DataSizeConverter.megabytesToBytes(10);        // 10,485,760

// Format decimals
String shortVal = DataSizeConverter.shortDouble(3.14159, 2);     // "3.14"
String localeVal = DataSizeConverter.shortDouble(3.14, 2, Locale.FRENCH); // "3,14"

// Max speed tracking (per-instance)
DataSizeConverter converter = new DataSizeConverter();
double maxUp = converter.getTopUploadSpeed(currentUpBytes);
double maxDown = converter.getTopDownloadSpeed(currentDownBytes);
converter.resetMaxSpeeds(); // New session
```

### JsonConverter - Snake_Case ↔ CamelCase

```java
// API returns snake_case → Java camelCase
String javaJson = JsonConverter.convertKeysToJavaCase("{\"user_name\":\"Sami\"}");
// {"userName":"Sami"}

// Java camelCase → API snake_case
String apiJson = JsonConverter.convertKeysToJsonCase("{\"userName\":\"Sami\"}");
// {"user_name":"Sami"}

// Standalone key conversion
String camel = JsonConverter.jsonToJavaCase("user_name"); // "userName"
String snake = JsonConverter.javaToJsonCase("userName");  // "user_name"
```

---

## 🌍 System

### LanguageHelper - App Locale

```java
// Get codes
String sysLang = LanguageHelper.getSystemLanguage(); // "en"
String appLang = LanguageHelper.getAppLanguage();    // "ar"

// Change app language at runtime
LanguageHelper.updateAppLanguage(context, "ar"); // Arabic
// Call in Activity.onCreate before setContentView()
```

### ServiceHelper - Check Service Running

```java
boolean running = ServiceHelper.isServiceRunning(context, MyForegroundService.class);
```

### RestartHelper - App Restart

```java
// Restart to launcher activity
RestartHelper.restartToMain(context, "ACTION_REFRESH");

// Restart to specific activity
Intent intent = new Intent(context, LoginActivity.class);
RestartHelper.restartToTarget(context, intent);
```

---

## 🧩 Managers

### InstancesManager - Singleton Registry

```java
// Register
InstancesManager.getInstance(activity).add(myRepository);

// Retrieve
MyRepository repo = InstancesManager.getInstance(activity).get(MyRepository.class);

// Replace
InstancesManager.getInstance(activity).replace(newRepo);

// Remove
InstancesManager.getInstance(activity).remove(MyRepository.class);
```

### FragmentsManager - Fragment Instance Cache

```java
FragmentsManager fm = FragmentsManager.getInstance(activity);
fm.add(myFragment);
Fragment cached = fm.get(MyFragment.class);
fm.replace(newFragment);
fm.remove(MyFragment.class);
```

---

## 📋 Quick Reference

| Helper | Package | Key Class |
|--------|---------|-----------|
| **Database** | `db` | `SqlPreferences`, `SimpleDB`, `SecurePreferences` |
| **Network** | `network` | `HttpClient`, `ConnectivityUtils`, `AddressHelper`, `WifiHelper` |
| **Crypto** | `crypto` | `CryptoUtil`, `HmacVerifier`, `Base64Helper` |
| **Notifications** | `notification` | `NotificationMaker` |
| **Alerts** | `alerts` | `AlertMaker` |
| **Timers** | `timers` | `CountdownTimer`, `ChronometerTimer` |
| **Converters** | `converters` | `DataSizeConverter`, `JsonConverter` |
| **System** | `system` | `LanguageHelper`, `ServiceHelper`, `RestartHelper` |
| **Managers** | `manager` | `InstancesManager`, `FragmentsManager` |
| **Runtime** | `runtime` | `RestartHelper` |

---

## ⚠️ Common Pitfalls

| Issue | Solution |
|-------|----------|
| `SqlPreferences` data not persisting | Call `.apply()` after `putXxx()` chain |
| `HttpClient` callback on background thread | Use `runOnUiThread()` for UI updates |
| `CountdownTimer` listener on timer thread | Post to main thread for UI |
| `WifiHelper.setWifiEnabled()` no-op on API 29+ | Use `WifiManager` system APIs or settings intent |
| `SimpleDB` encryption fails in prod | Check `MasterKey` deprecation; uses Harmony fallback |
| Notification not showing Android 13+ | Request `POST_NOTIFICATIONS` runtime permission |

---

*DroidHelpers v1.0.4 - MIT License*  
*https://github.com/rochdi-wafik/droidhelpers*