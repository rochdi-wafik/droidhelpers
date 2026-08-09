# DroidHelpers - Android Helpers Kit🚀

**DroidHelpers** A lightweight Android library packed with powerful utilities to simplify app development. It offers modular, easy-to-use helpers for **storage**, **networking**, **encryption**, **UI alerts**, **notifications**, **timers**, and more.

---

## ✨ Features

- Easy-to-use and modular helper classes
- Works across most Android API levels
- Reduces boilerplate code
- Lightweight and dependency-free (where possible)

---

## Included Helpers

### 🔔 UI Alerts

- `AlertMaker` - Build Bootstrap-style alert dialogs (toast/snackbar/alert dialog) with ease.

---

### 🔁 Converters

- `DataSizeConverter` - Convert bytes to human-readable size (KB, MB, etc.).
- `JsonConverter` - Serialize and deserialize JSON using native or third-party parser.

---

### 🔐 Crypto

- `Base64Helper` - Encode/decode strings and byte arrays to/from Base64.
- `CryptoUtil` - Encrypt/decrypt using AES, XOR, etc.
- `HmacVerifier` - Verify HMAC signatures

---

### 📦 Local Storage

- `SimpleDB` - SharedPreferences wrapper to save/get/delete **objects** or **lists** of objects.
- `SqlPreferences` - A SharedPreferences-like API, backed by SQLite for more flexibility.
- `SecurePreferences` - Encrypted & Thread /Multi Process safe

---

### 🌐 Network

- `AddressHelpers` - Utilities for working with IPs and domains.
- `HttpClient` - Simplified HTTP request interface (GET, POST, headers, etc.).
- `WifiHelper` - Manage and check WiFi and hotspot states (enabled, disabled, etc.).
- `ConnectivityUtils` - Check connection states like isConnected, hasInternet, etc.

---

### 🔔 Notifications

- `NotificationMaker` - Easily create modern and backward-compatible notifications with fewer lines of code.

---

### ⏱️ Timers

- `ChronometerTimer` - Track elapsed time easily.
- `CountdownTimer` - Countdown timer utilities.

---
### 🛠️ Usage Examples
- Visit [Examples.MD](/droidhelpers/docs/Examples.MD) for full examples.

## 📦 Installation

You can import this library into your project as a local module (git clone) or as dependency

### Option 1: Implement as dependency
```gradle
implementation 'com.github.rochdi-wafik:droidhelpers:x.x.x'
```
### Option 2: Import as Module

1. Clone or download this repo
2. Add it to your project via `File > New > Import Module`
3. Add it as a dependency in `build.gradle`:

```gradle
implementation project(":droidhelpers")
```


---

## 🔧 Compatibility
* Minimum SDK: API 29
* Language: Java (compatible with Kotlin-based projects)

[![](https://jitpack.io/v/rochdi-wafik/droidhelpers.svg)](https://jitpack.io/#rochdi-wafik/droidhelpers)

## Developer Guide
- Visit [DeveloperGuide.MD](/droidhelpers/docs/Developer_Guide.MD)

--- 

## 📄 License
MIT License - feel free to use, modify, and distribute with attribution.

