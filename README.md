# SKYRAS TV

**Original IPTV Player Application for SKYRAS Media**  
**Owner**: Travis Singletary  

SKYRAS TV is an original IPTV client engineered for Android TV, Amazon Fire TV, and mobile devices. It pairs high-performance stream playback (AndroidX Media3 / ExoPlayer core) with modern natural-language discovery through an AI Assist panel, plus modular affiliate and community slots.

---

## Highlights & Features

1. **100% Original Codebase**: Built completely clean-room from scratch. No code, assets, or decompiled resources from TVMate or other proprietary IPTV apps.
2. **Permissive Open-Source Foundation**: Strictly Apache-2.0 and MIT components (see `LICENSES.md`). Zero GPL copyleft code.
3. **Core IPTV Capabilities**:
   - M3U and M3U8 playlist import with full EXTINF parsing (`tvg-id`, `tvg-name`, `tvg-logo`, `group-title`, `tvg-chno`, `#EXTGRP`).
   - Xtream Codes API integration (`player_api.php`) for Live TV streams, VOD, and Series.
   - XMLTV EPG parser supporting channel mappings, air dates, program descriptions, and current airing calculations.
   - Favorites management, search, and category grouping.
4. **Feature 1 — AI Assist Panel (Fully Built)**:
   - Natural-language queries (e.g. *"find me the game"*, *"what's on right now?"*, *"recommend a movie"*).
   - Ingests user channel lineup and live EPG airings into contextual LLM prompts.
   - Pluggable `LlmProvider` interface: comes with an offline `MockLlmProvider` and a configurable `OpenAiCompatibleProvider` (OpenAI, Ollama, Groq, LocalAI). No hardcoded keys.
5. **Feature 2 — Sports Picks / Bet Affiliate Slot (Placeholder Stub)**:
   - Built on an affiliate model linking out to licensed sportsbooks (DraftKings, FanDuel) with tracking tags.
   - **Zero real-money wagering logic** inside the app.
   - Kept behind a feature flag (defaults to OFF).
   - Includes mandatory `TODO`s and enforcement gates for Geolocation gating and 21+ Age verification.
6. **Feature 3 — Community Slot (Spec Update)**:
   - Nav entry, route, and empty placeholder screen for community engagement.
   - Configurable destination URL (default: `https://discord.gg/skyras`) with QR code notice.
   - Controlled via feature flag.
7. **D-Pad TV Navigation & Companion Player**:
   - 10-foot UI focus controller supporting Arrow Keys (Up/Down/Select) and TV remote navigation.
   - Web companion simulator serving HLS playback on port 43128 with live mock Xtream endpoints.

---

## Project Structure

```
skyras-tv/
├── core-model/                 # Domain entities (Channel, EpgProgram, VodItem, SeriesItem)
├── core-iptv/                  # M3U/M3U8 & XMLTV Parsers, PlaylistRepository
├── core-xtream/                # Xtream Codes Client & DTOs
├── feature-ai-assist/          # AI Assist Engine, Pluggable LlmProvider & Mock Provider
├── feature-sports-picks/       # Affiliate linkout stub with Geo/Age-gate checks
├── feature-community/          # Community slot placeholder & route controller
├── app-tv/                     # TV Focus Controller & Media3 playback abstractions
├── web-preview/                # Interactive TV web companion & mock Xtream server
├── ARCHITECTURE.md             # In-depth architectural design and platform justification
└── LICENSES.md                 # Complete license audit of all dependencies
```

---

## Build & Test Instructions

### Prerequisites
- JDK 17+ (JDK 21 is fine)
- Android SDK (compile SDK 35) for the Fire TV APK
- Node.js v18+ (for web companion preview)

### Build a Fire Stick / Android TV APK
The installable app lives in `:app-tv`. There is no production keystore in this repo, so the release APK is signed with the debug key for sideload.

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties   # gitignored
./gradlew :app-tv:assembleRelease
```

APK path:

```
app-tv/build/outputs/apk/release/app-tv-release.apk
```

Sideload on Fire TV with ADB or AFTVnews Downloader after you host that APK on a public HTTPS URL.

### 1. Run Kotlin Multi-Module Unit Tests
Execute the complete test suite across all subprojects:
```bash
./gradlew test --info
```

All subprojects include hermetic unit tests:
- `:core-iptv:test` -> M3U parsing, BOM stripping, EXTGRP, and XMLTV EPG parsing.
- `:core-xtream:test` -> Xtream Codes authentication, categories, and live streams against MockWebServer.
- `:feature-ai-assist:test` -> AI query processing, channel extraction, MockLlmProvider, and OpenAI-compatible endpoint.
- `:feature-sports-picks:test` -> Affiliate URL generation, feature-flag enforcement, geo-gate/age-gate checks.
- `:feature-community:test` -> Community route state and configurable target URL.
- `:app-tv:test` -> TV Focus Controller, D-Pad navigation, and channel selection.

### 2. Run the Interactive TV Web Companion
Launch the companion server:
```bash
cd web-preview
node server.js
```
Open your browser at `http://127.0.0.1:43128` to experience the 10-foot TV interface, test D-Pad keyboard navigation (`▲`, `▼`, `Enter`), try AI assist queries, and test feature flags.

Run the companion verification tests:
```bash
node web-preview/test.js
```
