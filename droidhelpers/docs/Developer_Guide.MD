# DroidHelpers - Architecture & Maintenance Map 🗺️

This document serves as a living map for current and future maintainers of the `droidhelpers` library. It outlines the package structure, core responsibilities, etc.

## 📁 Project Structure

```
droidhelpers/                          # Root project (Gradle wrapper, settings)
├── droidhelpers/                      # Library module (AAR output)
│   ├── docs/ Developer_Guide.MD, Examples.MD, Publishing.MD
│   ├── src/main/java/com/iorgana/droidhelpers/
│   │   ├── alerts/        AlertMaker.java
│   │   ├── converters/    JsonConverter, DataSizeConverter
│   │   ├── crypto/        CryptoUtil, Base64Helper, HmacVerifier
│   │   ├── db/            SqlPreferences, SimpleDB, SecurePreferences
│   │   ├── manager/       InstancesManager, FragmentsManager
│   │   ├── network/       HttpClient, ConnectivityUtils, AddressHelper, WifiHelper
│   │   ├── notification/  NotificationMaker
│   │   ├── runtime/       RestartHelper
│   │   ├── service/       ServiceHelper
│   │   ├── system/        LanguageHelper
│   │   ├── timers/        CountdownTimer, ChronometerTimer
│   │   ├── utils/         Utils, JPatterns, StreamUtils
│   │   ├── converters/    DataSizeConverter, JsonConverter
│   │   └── R.java         (generated resources)
│   ├── src/main/res/
│   │   ├── xml/           backup_rules.xml, data_extraction_rules.xml
│   │   └── etc/  other res files
│   ├── build.gradle       # Library config (compileSdk, minSdk, Java Version)
│   ├── ChangeLog.md       # Version history
│   ├── proguard-rules.pro # R8/ProGuard rules for library consumers
│   └── consumer-rules.pro # Consumer-side ProGuard rules
├── app/                     # Sample app (manual testing)
├── gradle/wrapper/          # Gradle 8.x wrapper
├── build.gradle             # Root plugins (AGP 9.2.1)
├── gradle.properties        # JVM args, version constants
└── jitpack.yml              # JitPack publish config
```
### Package Organization
```
com.iorgana.droidhelpers.<domain>
├── alerts/       → UI components (dialogs, toasts)
├── converters/   → Data transformation (JSON, bytes)
├── crypto/       → Encryption, hashing, signing
├── db/           → Persistence (SQLite, SharedPrefs)
├── managers/     → Object lifecycle (singletons, fragments)
├── network/      → HTTP, connectivity, IP/DNS utilities
├── notification/ → Notification builder
├── runtime/      → App lifecycle (restart)
├── service/      → Service utilities
├── system/       → System settings (i.e locale)
├── timers/       → Countdown, chronometer
└── utils/        → Shared utilities, patterns
```
---

## 🏗️ Architecture & Design Principles

### Core Principles
| Principle | Description |
|-----------|-------------|
| **Zero-config defaults** | Works out of the box; customization optional |
| **Main-thread safe** | Caching + async I/O; no UI blocking |
| **Encryption by default** | AES-GCM (CryptoUtil), Android Keystore (SharedPrefs) |
| **Singleton per context** | `getInstance(Context)` pattern throughout |
| **Builder/Fluid API** | Method chaining for readability |
| **Java 17 + AndroidX** | Modern APIs, no legacy support burden |

---

## 🔧 Key Implementation Patterns

### 1. Singleton with Context (Thread-Safe)
Used in: `SqlPreferences`, `HttpClient`, `NotificationMaker`, `SimpleDB`, `InstancesManager`

```java
private static volatile MyClass INSTANCE;

public static MyClass getInstance(Context context) {
    if (INSTANCE == null) {
        synchronized (MyClass.class) {
            if (INSTANCE == null) {
                INSTANCE = new MyClass(context.getApplicationContext());
            }
        }
    }
    return INSTANCE;
}
```
> **Rule**: Always use `context.getApplicationContext()` to avoid leaks.

### 2. Async Init + Sync Fallback
Used in: `SqlPreferences.init()` / `initSync()`

```java
// Async init (recommended at app start)
public static void init(Context ctx, OnLoadListener listener) {
    executors.execute(() -> {
        INSTANCE.cache.putAll(INSTANCE.getAll());  // Load to memory
        if (listener != null) listener.onLoaded();
    });
}

// Sync fallback (called lazily from getInstance)
public void initSync() {
    if (cache.isEmpty()) cache.putAll(getAll());
}
```

### 3. Write-Behind Cache Pattern (SqlPreferences)
- **Temp Map**: `putXxx()` calls buffer into `tempMap`
- **apply()**: Moves `tempMap → cache` (instant), then async writes to SQLite
- **Read**: Always from `cache` (ConcurrentHashMap) - main thread safe

### 4. Builder Pattern (Fluid API)
All configuration classes: `AlertMaker`, `NotificationMaker`, `HttpClient` headers, `CountdownTimer`

```java
new AlertMaker(ctx).setType(Success).setTitle("Hi").setContent("Done").build();
```

### 5. Callback on Background Thread
**Documentation convention**: "Listener invoked on background thread - switch to main for UI"

```java
public interface TimerListener {
    void onChange(int seconds);  // Background thread!
}
```

---

## 🗃️ Database Internals (SqlPreferences)

### Schema
```sql
CREATE TABLE table_preferences (
    data_key    TEXT PRIMARY KEY,   -- "username", "pref_obj_user", "pref_list_obj_com.pkg.Userfriends"
    data_type   TEXT,               -- "String", "Integer", "Boolean", "Long", "Float", "Double"
    data_value  TEXT                -- Encrypted value (or plain if disabled)
);
```

### Key Prefixes
| Type | Prefix | Example Key |
|------|--------|-------------|
| Primitive | (none) | `"username"` |
| Object | `pref_obj_` | `"pref_obj_user"` |
| List<Object> | `pref_list_obj_` + className | `"pref_list_obj_com.pkg.User_friends"` |

### Concurrency Model
- **Cache**: `ConcurrentHashMap<String, Object>` - read/write from any thread
- **DB Writes**: `ExecutorService` (fixed 4 threads) - `insertMap()` runs async
- **DB Reads**: `getAll()` sync (called once at init), `remove()` async

### Lifecycle
1. `Application.onCreate()` → `SqlPreferences.init(ctx, callback)`
2. Background thread loads all rows → `cache.putAll()`
3. `getInstance()` → calls `initSync()` if cache empty (blocks UI)
4. `putXxx()` → buffers in `tempMap`
5. `apply()` → `cache.putAll(tempMap)`, `tempMap.clear()`, `executor.execute(insertMap(copy))`

---

## 🌐 Network Layer (HttpClient)

### OkHttp Configuration
```java
new OkHttpClient.Builder()
    .connectTimeout(15, SECONDS)
    .readTimeout(20, SECONDS)
    .writeTimeout(20, SECONDS)
    .build();
```

### Request Flow
```
HttpClient.getInstance()           // Singleton, clears headers
    .addHeaders(map)               // Optional headers
    .get(url, callback)            // Async enqueue
    .getSync(url)                  // Blocking execute()
    .postJson(url, json, callback)
    .postJsonSync(url, json)
```

### Callback Contract
```java
interface ICallback {
    void onSuccess(byte[] body, Headers headers);  // ANY HTTP status (2xx, 4xx, 5xx)
    void onError(Exception e);                      // Transport failure only
}
```
> **Important**: Server errors (4xx/5xx) arrive in `onSuccess` - caller must parse body for error codes.

### Request Tracking
```java
client.countRunningRequests();   // Active
client.countQueuedRequests();    // Waiting
client.countPendingRequests();   // Running + Queued
client.cancelAllRequests();      // Dispatcher.cancelAll()
client.shutdown();               // Release pools, nullify singleton
```

---

## 🔔 NotificationMaker Internals

### Two Modes
| Mode | Constructor | ID | Use Case |
|------|-------------|-----|----------|
| **Single** | `new NotificationMaker(ctx)` | Random | One-off notifications |
| **Shared** | `NotificationMaker.getInstance(ctx)` | Fixed | Foreground services, ongoing ops |

### Channel Creation
```java
// Created on first create()
NotificationChannel channel = new NotificationChannel(CHANNEL_ID, title, IMPORTANCE_DEFAULT);
channel.setDescription(content);
manager.createNotificationChannel(channel);
```

### Permission Handling (Android 13+)
```java
public void show() {
    if (!areNotificationsEnabled()) return;  // Silent no-op
    // ... build & notify
}
```
No exception thrown - callers should check `areNotificationsEnabled()` if they need to prompt.

---

## 🛠️ Maintenance Checklist

### ✅ When Bumping Target SDK (e.g., to API 37)
1. **PendingIntent Flags**: Search for `PendingIntent.get...` and ensure `FLAG_IMMUTABLE` or `FLAG_MUTABLE` is explicitly set (required since API 31, strictly enforced later).
2. **Notification Permissions**: Verify `NotificationMaker` gracefully handles the `POST_NOTIFICATIONS` runtime permission (API 33+).
3. **Foreground Services**: Check `ServiceHelper` to ensure it declares the correct `android:foregroundServiceType` in the manifest and requests runtime permissions if needed (API 34+).
4. **Background Execution**: Ensure `HttpClient` and `SqlPreferences` do not trigger background execution limits. Consider migrating heavy tasks to `WorkManager` wrappers if needed.

### ✅ Security Audit (Quarterly)
- [ ] Verify `CryptoUtil` uses secure algorithms (e.g., `AES/GCM/NoPadding` instead of `AES/ECB/PKCS5Padding`).
- [ ] Ensure `SecurePreferences` is not just Base64 encoding, but actual encryption (preferably via `EncryptedSharedPreferences`).
- [ ] Check `HmacVerifier` for timing-attack vulnerabilities (use `MessageDigest.isEqual` for byte array comparison).

