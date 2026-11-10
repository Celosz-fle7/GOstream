# GOstream — High-Performance Native Android & Android TV Streaming App

**GOstream** adalah aplikasi streaming native modern berbasis **Kotlin** dan **AndroidX Media3 (ExoPlayer)** dengan arsitektur **Zero-Disk Bloat** (konsumsi penyimpanan < 50 MB) dan dukungan antarmuka adaptif untuk **Handphone** dan **Android TV** (D-Pad remote navigation).

---

## Fitur Utama

- **Zero-Disk Video Buffering:**
  - Menggunakan RAM ring-buffer Media3 (`DefaultLoadControl` 15s - 30s di memori).
  - Video tidak disimpan ke disk internal HP, menjaga ruang memori tetap bersih dan awet.
- **Dual Form-Factor Support:**
  - **Android Mobile:** Grid 3 kolom responsif, touch navigation, adaptive layout.
  - **Android TV:** Grid 5-6 kolom, fokus D-Pad remote visual highlight (`bg_card_focus`), kontrol tombol remote (Left/Right seek 10s, Center play/pause).
- **Multi-Provider Catalog Engine:**
  - Terintegrasi dengan model provider streaming autentik (Moviebox, Anichin, Drama ID, Samehadaku, Live TV, dll.).
  - Automatic Bearer token rotation & session handshake.
- **Auto-Build CI/CD via GitHub Actions:**
  - Setiap push kode ke `main` otomatis di-compile menjadi file APK siap pakai di server GitHub.

---

## Arsitektur Teknis

- **Language:** Kotlin 1.9.23
- **Android Target:** Min SDK 24 (Android 7.0+) | Target SDK 34 (Android 14)
- **Player Core:** `androidx.media3:media3-exoplayer:1.3.1` (HLS, DASH, Progressive MP4)
- **Network Engine:** OkHttp 4.12.0 + Gson
- **Image Pipeline:** Coil 2.6.0 (Memory-optimized, cap disk cache @ 15MB)
- **Build System:** Gradle 8.6 + Android Gradle Plugin 8.3.2

---

## Cara Unduh APK

1. Masuk ke repositori GitHub: [Celosz-fle7/GOstream](https://github.com/Celosz-fle7/GOstream)
2. Klik tab **Actions**.
3. Pilih workflow run terbaru pada workflow **Build Android APK**.
4. Di bagian bawah (Artifacts), download **`GOstream-debug-apk`**.
5. Install file `.apk` ke HP atau Android TV kamu!
