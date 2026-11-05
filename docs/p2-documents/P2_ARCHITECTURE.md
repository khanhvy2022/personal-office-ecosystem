# P2.1 — EW DOCUMENTS ARCHITECTURE & DESIGN SPECIFICATION

**Project:** EW Personal Office Ecosystem  
**Application:** EW Documents (`com.eurowindow.documents`)  
**Phase:** P2 — Standalone DOCX Editor Prototype  
**Baseline Commit:** `0edd8ea`  
**Current Branch:** `p2-documents-prototype`  
**Date:** 2026-11-05  

---

## 1. Executive Architecture Summary

EW Documents is a standalone, offline-first Android application designed specifically for reading, creating, and editing Microsoft Word documents (`.docx`, `.doc`) and OpenDocument Text (`.odt`). It operates as an independent APK without requiring EW Sheets (`com.eurowindow.sheets`) or any external cloud, account, advertising, or telemetry services.

The application combines a modern **Jetpack Compose Material 3** application shell with an embedded **Collabora Online / LibreOfficeKit** native C++ engine running via local JNI and loopback UNIX sockets.

```
+---------------------------------------------------------------------------------+
|                                 EW DOCUMENTS                                    |
|                         (com.eurowindow.documents)                              |
+---------------------------------------------------------------------------------+
|  Application Shell (Jetpack Compose / Material 3)                               |
|  - DocumentsPrototypeActivity & DocumentsMainScreen                             |
|  - Navigation: Tài liệu (Documents) | Cài đặt (Settings)                        |
|  - Recent Documents Database (ew_documents.db via Room/SQLite)                  |
|  - Built-in Eurowindow Test Corpus Access (DOCX 01 - 04)                        |
+---------------------------------------------------------------------------------+
|  Storage Access Framework (SAF) Controller                                      |
|  - ACTION_OPEN_DOCUMENT (Read/Write/Persistable URI grants)                     |
|  - ACTION_CREATE_DOCUMENT (MIME_DOCX)                                           |
|  - Ephemeral cache working-copy management via TempFileGuard                   |
+---------------------------------------------------------------------------------+
|  Native Editor Host (LOActivity / COWebViewClient)                              |
|  - Loopback IPC (cool://localhost/cool/mobilesocket/...)                       |
|  - Offline-Only URL Policy (EWOfflineUrlPolicy)                                 |
|  - Dynamic Font Configuration (/data/data/com.eurowindow.documents/fontconfig)  |
+---------------------------------------------------------------------------------+
|  Collabora Native Core (ARM64-v8a Native Shared Libraries)                      |
|  - liblo-native-code.so (LibreOfficeKit C++ Engine)                            |
|  - libandroidapp.so, libc++_shared.so, libsqlite3.so, libnss3.so                |
+---------------------------------------------------------------------------------+
```

---

## 2. Module Composition & Isolation

EW Documents strictly follows modular boundaries. It depends on internal shared core modules at build-time while maintaining strict runtime isolation:

| Module | Type | Purpose | Isolation Guarantee |
| :--- | :--- | :--- | :--- |
| `:app:documents` | Android Application | UI Shell, ViewModels, Launcher Activity, App Branding | Isolated sandbox (`/data/data/com.eurowindow.documents/`) |
| `:engine:collabora-runtime` | Android Library | Native C++ `.so` libraries, LOKit bridge, UNO assets, fonts | Parameterized `APPLICATION_ID` dynamic configuration |
| `:core:storage` | Android Library | SAF integration, display name resolver, URI safety | No shared file storage with EW Sheets |
| `:core:database` | Android Library | SQLite Recent Document Store (`ew_documents.db`) | App-private database file |
| `:core:datastore` | Android Library | Preferences Store (`ew_documents_prefs`) | App-private SharedPreferences / DataStore |
| `:core:security` | Android Library | Manifest merger rules, URL policy, cache scrubbers | S1–S4 remediation enforcement |
| `:core:ui` | Android Library | Material 3 Theme tokens, color palettes, typography | Stateless build-time UI resources |

### Strict Isolation vs EW Sheets:
1. **Application ID:** `com.eurowindow.documents` vs `com.eurowindow.sheets`.
2. **FileProvider Authority:** `com.eurowindow.documents.fileprovider` vs `com.eurowindow.sheets.fileprovider`.
3. **Database Name:** `ew_documents.db` vs `ew_sheets.db`.
4. **DataStore Name:** `ew_documents_prefs` vs `ew_sheets_prefs`.
5. **Clipboard Authority:** `com.eurowindow.documents.clipboard` vs `com.eurowindow.sheets.clipboard`.
6. **FontConfig Path:** `/data/data/com.eurowindow.documents/fontconfig` vs `/data/data/com.eurowindow.sheets/fontconfig`.

---

## 3. UI Shell Architecture (Jetpack Compose)

The application shell is built entirely in **Jetpack Compose** following Material Design 3 guidelines with corporate Eurowindow styling:

### 3.1 Design Tokens & Branding
- **Primary Brand Color:** Deep Navy (`#0F2E5A` / `0xFF0F2E5A`).
- **Secondary Accent:** Electric Blue (`#2260B4` / `0xFF2260B4`).
- **Background Surface:** Clean Neutral Surface (`#F8F9FA` light mode).
- **Brand Identity:** Distinct "W" monogram badge for Eurowindow Documents.

### 3.2 Main Screen Components (`DocumentsMainScreen.kt`)
1. **Top App Bar:**
   - App title "EW Documents" with version badge `v0.1.0-p2`.
   - Network status indicator ("Ngoại tuyến" / Strictly Offline).
2. **Tab Navigation:**
   - **Tài liệu (Documents Tab):** Primary productivity screen.
   - **Cài đặt (Settings Tab):** Configuration and maintenance tools.
3. **Quick Action Panel:**
   - **Mở tệp (SAF):** Triggers `ACTION_OPEN_DOCUMENT` for existing `.docx`, `.doc`, `.odt` files.
   - **Tạo DOCX mới:** Triggers `ACTION_CREATE_DOCUMENT` to create a blank document directly in user storage.
4. **Recent Documents List:**
   - Reads directly from `RecentDocumentStore`.
   - Displays file title, path/URI hint, last opened timestamp, and document type icon.
   - Action menu allows opening or removing entries from the recent list.
   - Shows empty-state illustration when no documents have been opened yet.
5. **Sample Corpus Quick Launcher:**
   - One-tap access to 4 verified Eurowindow test documents (Corpus 01–04) for quick validation.
   - Materializes assets into isolated app cache before passing to editor.
6. **Settings Screen:**
   - Offline Mode Status banner (Confirmed 0 network access).
   - Spellcheck disable toggle (`EWPreferencesStore.isSpellCheckDisabled`).
   - Ephemeral cache cleanup trigger (`TempFileGuard.scrubAppCacheDirectory`).
   - System and toolchain version metadata.

---

## 4. SAF Document Workflow

To respect user privacy and adhere to Android 14+ storage requirements (S2 remediation), EW Documents **never** requests `MANAGE_EXTERNAL_STORAGE` or broad `READ/WRITE_EXTERNAL_STORAGE` permissions. All document access flows through the Android Storage Access Framework:

```
[User Action]
     |
     +---> "Mở tệp" ---> SafDocumentController.buildOpenDocumentIntent()
     |                          |
     |                          v
     |                   ACTION_OPEN_DOCUMENT (MIME: .docx, .doc, .odt)
     |                          |
     |                          v
     |                   Android System Document Picker
     |                          |
     |                          v
     |                   onActivityResult(Uri)
     |                          |
     |                          v
     |                   contentResolver.takePersistableUriPermission(read/write)
     |                          |
     |                          v
     |                   RecentDocumentStore.recordDocument(uri, name)
     |                          |
     |                          v
     +---> "Chỉnh sửa" -> Launch org.libreoffice.LOActivity(uri)
```

---

## 5. Native Engine Integration Lifecycle

The Collabora native editor is encapsulated within `org.libreoffice.LOActivity`:
1. **Initialization:** On activity creation, `LOActivity` configures the native environment, loads `liblo-native-code.so`, and unpacks necessary UNO runtime assets.
2. **FontConfig Initialization:** The dynamic font configuration (`fonts.conf`) is localized to `/data/data/com.eurowindow.documents/fontconfig/` ensuring font isolation.
3. **WebView Canvas:** The document UI is rendered via a hardened `COWebViewClient` that strictly allows internal `cool://` and `file:///android_asset/` URLs and drops any external network request.
4. **Termination & Cleanup:** When the editor activity finishes, native allocations are released, and `TempFileGuard` scrubs any lingering temporary print PDFs or clipboard fragments from the cache.

---

## 6. Architecture Verification Status

- [x] Application shell compiles cleanly with Compose 1.7+.
- [x] SAF controller passes all automated URI intent and permission checks.
- [x] Recent documents database correctly isolates data in `ew_documents.db`.
- [x] Strict namespace isolation verified against EW Sheets.
- [x] Zero network permissions verified in merged manifest.
