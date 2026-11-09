# FLUX — Next-Generation Activity-First Native Android Launcher

**FLUX** (`com.aistudio.fluxlauncher.v9x2k`) is a native Android home-screen launcher and personal activity environment built with **Kotlin**, **Jetpack Compose**, **Room**, **DataStore**, and **Android AppWidgetHost**.

Instead of forcing users into a static icon grid or alphabetical drawer, FLUX organizes your digital life around **Continuity Activities**, **Spaces**, **Object-Centric Contextual Actions**, and an intent-driven **Action Field**.

---

## Core Architecture & Features

1. **Genuine Android Launcher Registration (`HOME` & `DEFAULT`)**
   - `MainActivity` is registered with `android.intent.action.MAIN`, `android.intent.category.HOME`, `android.intent.category.DEFAULT`, and `android.intent.category.LAUNCHER` (`launchMode="singleTask"`, `stateNotNeeded="true"`).
   - Pressing the hardware/gesture Home button returns immediately to the FLUX Living Environment.
   - Supports incoming `Intent.ACTION_SEND` shares (`text/plain`, `image/*`, `application/pdf`) to attach links, notes, or files directly into an active workspace.

2. **Home Screen: The Living Environment**
   - Displays current date, high-contrast `Space Grotesk` clock, live/cached Open-Meteo atmospheric weather glance, and a contextual status capsule.
   - Displays **Continuity Activities** with progress indicators and interactive connected object chips (`PDFs`, `Notes`, `Screenshots`, `Links`, `Checklists`, `Shortcuts`).
   - Includes Pinned People shortcuts, Recent Continuity history, and an optional customizable Quick Access Dock.

3. **Spaces Environment**
   - Pre-configured with **Personal**, **Study**, **Work**, **Media**, **People**, and **Games** spaces, plus user-created custom Spaces.
   - Dedicated **Study & Work Spaces** with searchable document references, tasks, and deadlines.
   - Dedicated **People Space** with contact group filters (`Starred`, `Study Group`, `Team`, `Family`), `DIAL`/`SMS`/`EMAIL` intents, and optional `READ_CONTACTS` integration.
   - Dedicated **Media Space** with category filters (`Photos`, `Music`, `Videos`, `Files & PDFs`) using Android Scoped Storage URI references without duplicating files.

4. **The Action Field & Universal Search**
   - Prompt: *"What do you want to do?"* with text search, voice recognition (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`), and category filters (`All`, `Apps`, `Actions`, `Files`, `Settings`, `Web`).
   - Intent-oriented pills (`Create Note`, `Open File`, `New Activity`, `Plan Task`, `Send / Share`, `Media Space`) and dismissible continuity suggestions.

5. **Object-Centric Contextual Actions & Content Surface**
   - Tapping or long-pressing any object opens the Contextual Action Sheet (`Open`, `Share`, `Edit / Move`, `Copy / Extract Text`, `Wallpaper`, and local Assistant actions).
   - Opening an activity launches its dedicated **Content Surface** workspace with grouped objects, compact/spatial toggle, and task checklists.

6. **Interactive Widgets (`AppWidgetHost` + Native Widgets)**
   - Hosts genuine third-party Android home-screen widgets via `AppWidgetHost` (`HOST_ID = 2048`) and `AppWidgetManager`.
   - Includes 5 interactive native FLUX widgets: Daily Continuity Pulse, Open-Meteo Weather Glance, Media Session Controller, Active Checklist, and Device Energy Ring.

7. **Customization Studio, Visual Layout Editor & Gesture System**
   - Themes: `Charcoal Dark`, `AMOLED Pure Black`, `Editorial Light`, and `Follow System`, plus 6 accent palettes, surface opacity, corner radius, icon shape masks (`Squircle`, `Circle`, `Sharp Square`, `Hexagon`, `Teardrop`), and wallpaper integration.
   - Visual Layout Editor with drag-offset handles, card size controls, section visibility toggles, preview mode, and default layout restoration.
   - Configurable Gestures (`Swipe up`, `Swipe down`, `Swipe left/right`, `Double tap`, `Pinch in`, `Pinch out`, `Long press`).

8. **Settings, Privacy & Local Assistant**
   - Searchable 11-section Settings & Privacy screen with JSON configuration backup/restore, history clearing, and an optional 100% on-device Local Assistant.

---

## Building and Installing the APK

### Prerequisites
- JDK 17+
- Android SDK (API 36 compileSdk, minSdk 26)

### Command-Line Build
```bash
# Run unit and Robolectric acceptance tests
gradle :app:testDebugUnitTest

# Build installable Debug APK
gradle :app:assembleDebug
```

The compiled debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing & Selecting FLUX as Default Home Launcher
1. Install `app-debug.apk` onto your Android phone.
2. Open **FLUX** and tap **"Set FLUX as Default Home"** (or open **Android Settings → Apps → Default apps → Home app** and select **FLUX**).
