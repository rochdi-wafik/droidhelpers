# DroidHelpers - Android Helpers Kit🚀

**DroidHelpers** A lightweight Android library packed with powerful utilities to simplify app development. It offers modular, easy-to-use helpers for **storage**, **networking**, **encryption**, **UI alerts**, **notifications**, **timers**, and more.

---

## ✨ Features

- ✅ Easy-to-use and modular helper classes
- ✅ Works across most Android API levels
- ✅ Reduces boilerplate code
- ✅ Lightweight and dependency-free (where possible)

---

## 🧰 Included Helpers

### 🔔 UI Alerts

- `AlertMaker` – Build Bootstrap-style alert dialogs (toast/snackbar/alert dialog) with ease.

---

### 🔁 Converters

- `DataSizeConverter` – Convert bytes to human-readable size (KB, MB, etc.).
- `JsonConverter` – Serialize and deserialize JSON using native or third-party parser.

---

### 🔐 Crypto

- `Base64Helper` – Encode/decode strings and byte arrays to/from Base64.
- `StringCrypto` – Encrypt/decrypt using AES, XOR, etc.

---

### 📦 Local Storage

- `SimpleDB` – SharedPreferences wrapper to save/get/delete **objects** or **lists** of objects.
- `SqlPreferences` – A SharedPreferences-like API, backed by SQLite for more flexibility.

---

### 🌐 Network

- `AddressHelpers` – Utilities for working with IPs and domains.
- `HttpClient` – Simplified HTTP request interface (GET, POST, headers, etc.).
- `WifiHelper` – Manage and check WiFi and hotspot states (enabled, disabled, etc.).

---

### 🔔 Notifications

- `NotificationMaker` – Easily create modern and backward-compatible notifications with fewer lines of code.

---

### ⏱️ Timers

- `Chronometer` – Track elapsed time easily.
- `Countdown` – Countdown timer utilities.

---

## 📦 Installation

You can import this library into your project as a local module or via GitHub (JitPack integration coming soon).

### Option 1: Use ad dependency
```gradle
implementation 'com.github.yourusername:android-utils:1.0.0'
```
### Option 2: Import as Module

1. Clone or download this repo
2. Add it to your project via `File > New > Import Module`
3. Add it as a dependency in `build.gradle`:

```gradle
implementation project(":your_module_name")
```

---

## 🛠️ Usage Examples
### 💾 Database: SqlPreferences.class
Here is a simple usage of how to store primitive data
```java
SqlPreferences.getInstance(anyContext)
              .putString("my_name", "Sami")
              .putDouble("weight", 35,4)
              .putBoolean("isAdmin", true)
              .apply();
```
Here is a simple usage of how to store serializable object and list
```java
SqlPreferences.getInstance(anyContext)
              .putObject("user", new User(...))
              .putListObject("users", Arrays.asList(new User(..)))
              .apply();
```
- Note: Data will not be saved until .apply() is called

---

## 🔧 Compatibility
* Minimum SDK: API 21+ (Android 5)
* Language: Java (compatible with Kotlin-based projects)


--- 

## 📄 License
MIT License – feel free to use, modify, and distribute with attribution.

