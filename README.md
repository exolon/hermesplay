# HermesPlay 🚀📺

**HermesPlay** is a fully custom, toddler-proof local media player built natively for Android tablets. Designed from the ground up to solve the frustrations of generic video players, it provides a distraction-free, fully offline viewing experience for kids. 

Built entirely with **Jetpack Compose** and **ExoPlayer**, HermesPlay bypasses the need for internet connections, cloud syncing, or subscriptions by utilizing Android's Scoped Storage to read video files directly from the device's hard drive.

### ✨ Key Features
* **Metadata-First Discovery Engine:** Scans local directories for `.mp4` files and instantly matches them with localized `poster.jpg` or `thumb.jpg` files for lightning-fast UI rendering.
* **Persistent Disk Caching:** Remembers your folder structures across reboots using a custom SharedPreferences vault, resulting in zero-lag navigation.
* **Toddler-Proof UI:** * Massive touch targets and an adaptive grid layout.
  * True Immersive Fullscreen (hides system nav bars).
  * A 3-second long-press "Hide/Reveal" mechanic to keep specific folders out of sight.
* **Smart Binge Mode:** Automatically queues up the next 5 episodes in a folder so kids don't have to navigate menus between shorts, pausing after 5 to save battery.
* **Resume Playback Memory:** Seamlessly remembers the exact millisecond a video was exited and resumes instantly upon returning.
* **Parent HUD:** A subtle, unobtrusive clock and battery indicator overlaid on the main menu.

### 🛠 Tech Stack
* **Kotlin & Jetpack Compose** (100% declarative UI)
* **Media3 / ExoPlayer** (High-performance video rendering)
* **Coil** (Asynchronous image loading and video-frame extraction)
* **Coroutines & ViewModels** (Asynchronous background file scanning)
* **Gemini AI**
