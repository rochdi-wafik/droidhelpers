# Project Structure

> Memory foundation for `droidhelpers` sample/usage app. Read this before touching `app/`.

## TECH_STACK
- **Language:** Java 17, no Kotlin.
- **Build:** Gradle (AGP 9.2.1), `compileSdk 36`, `minSdk 28`, `targetSdk 36`.
- **Modules:** `:droidhelpers` (library, already implemented) + `:app` (sample/demo app, this work).
- **UI:** Material Components (`com.google.android.material:material:1.14.0`), ViewBinding enabled, XML layouts, **no RecyclerView-less shortcuts** — MainActivity index uses `RecyclerView` + `Adapter` (per decision below).
- **Library deps already present:** OkHttp 5.4 (networking), Gson 2.14 (serialization), androidx.security-crypto, Harmony (encrypted, multi-process prefs), commons-io, orhanobut Logger.
- **No new third-party dependencies added** for the demo app — everything is built with what's already in `build.gradle`.

## Scope & Decisions (confirmed with stakeholder)
1. **Coverage depth:** Exhaustive — every public method (or the smallest set of overloads needed to represent every distinct capability) of every helper class gets a live, tappable demo.
2. **MainActivity index:** `RecyclerView` + `Adapter` (`HelperBoxAdapter`) rendering a fixed list of `HelperBox` items — not static views, to keep it open for future helper packages.
3. **Grouping:** 9 "boxes" = 9 detail Activities. Small/misc packages (`runtime`, `service`, `stream`, `system`, `utils`, `utils.JPatterns`) are merged into one **System & Utils** box instead of getting 6 separate Activities.

## SYSTEM_FLOW

### Navigation Map
```
SplashScreen
   └─> MainActivity  (index / "book cover")
          RecyclerView<HelperBox> — 9 cards, each: header (title) + body (classes it wraps) + footer button "Open"
          │
          ├─ Storage        -> StorageActivity        (SqlPreferences, SimpleDB, SecurePreferences)
          ├─ Networking     -> NetworkActivity         (HttpClient, AddressHelper, ConnectivityUtils, WifiHelper)
          ├─ Encryption     -> CryptoActivity          (CryptoUtil, HmacVerifier, Base64Helper)
          ├─ UI Alerts      -> AlertsActivity          (AlertMaker)
          ├─ Notifications  -> NotificationsActivity   (NotificationMaker)
          ├─ Timers         -> TimersActivity          (CountdownTimer, ChronometerTimer)
          ├─ Converters     -> ConvertersActivity      (JsonConverter, DataSizeConverter)
          ├─ App Managers   -> ManagersActivity        (FragmentsManager, InstancesManager)
          └─ System & Utils -> SystemUtilsActivity     (RestartHelper, ServiceHelper, StreamUtils,
                                                         LanguageHelper, Utils, JPatterns)
```

### Common Pattern (used by all 9 detail Activities)
All detail Activities extend `ui.base.BaseDemoActivity`, which owns:
- A scrollable root (`ScrollView` + vertical `LinearLayout`) built once in code.
- `addSection(title, subtitle)` → a `MaterialCardView` block per wrapped class (e.g. "SqlPreferences").
- `addResultRow(section, label, action)` → one row per public method: a button that runs the real
  `droidhelpers` call and an output `TextView` beneath it showing the actual return value/result,
  so the code shown IS the working usage example (not a mock).
- `addInput(section, hint, prefill)` → an `EditText` when a method needs user-provided data
  (URLs, keys, text to encrypt, etc.), pre-filled with a sane default so every demo is tap-and-go.
- A single Up/Back button in the ActionBar (standard `AppCompatActivity` behavior) back to MainActivity.

This keeps every Activity's code to "one class = one section = N method rows", so adding a 10th
helper package later only means: 1 new Activity extending BaseDemoActivity + 1 new HelperBox entry.

### Files Added (new files only — module untouched)
```
app/src/main/java/com/iorgana/droidhelpers_project/ui/
  model/HelperBox.java
  adapter/HelperBoxAdapter.java
  base/BaseDemoActivity.java
  demo/StorageActivity.java
  demo/NetworkActivity.java
  demo/CryptoActivity.java
  demo/AlertsActivity.java
  demo/NotificationsActivity.java
  demo/TimersActivity.java
  demo/ConvertersActivity.java
  demo/ManagersActivity.java
  demo/SystemUtilsActivity.java
app/src/main/res/layout/
  activity_main.xml        (rewritten: RecyclerView index)
  item_helper_box.xml       (new: card layout for each box)
app/src/main/AndroidManifest.xml  (register 9 new Activities + POST_NOTIFICATIONS permission)
```

## Notes / Assumptions
- Each demo mutates real app state (real SQLite DB, real encrypted prefs, real notifications) —
  this is intentional: it's a working reference implementation, not a mock.
- `NotificationsActivity` requests `POST_NOTIFICATIONS` at runtime (API 33+) before demoing `show()`.
- `CryptoUtil.verifyRSASignature()` demo generates a throwaway RSA keypair in-memory (Android
  `KeyPairGenerator`) to sign sample data, since the method needs a real keypair to be meaningful.
- `HttpClient` demos call `https://jsonplaceholder.typicode.com` (public test API) as the sample
  endpoint for GET/POST calls — swappable via the on-screen URL field.
- Related overloads that differ only by optional `headers`/`requestId` params are demoed through
  one row using the fullest overload (with the simpler overload noted in a code comment) instead
  of one row per overload, to keep each screen usable rather than a wall of near-duplicate buttons.
