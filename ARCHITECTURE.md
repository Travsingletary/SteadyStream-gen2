# SKYRAS TV Architecture Specification

**Product**: SKYRAS TV  
**Organization**: SKYRAS Media  
**Owner**: Travis Singletary  
**License**: Proprietary App Codebase / Permissive Open-Source Dependencies  

---

## 1. Executive Summary & Chosen Core Platform

### Platform Strategy: Android TV & Fire TV (Kotlin + AndroidX Media3 / ExoPlayer)

SKYRAS TV is architected targeting **Android TV, Fire TV, and Android mobile devices**, matching the target form factors and 10-foot living room experience. 

#### Why AndroidX Media3 / ExoPlayer as the Core Player Engine:
1. **Industry Benchmark for Adaptive Streaming**: AndroidX Media3 (ExoPlayer) provides native support for HLS (`.m3u8`), MPEG-DASH, and HTTP Live Streams (`.ts`, `.mp4`, `.mkv`), which are the dominant formats for IPTV, live events, and on-demand video.
2. **Hardware Acceleration & Codec Support**: Native hardware decoding for AVC/H.264, HEVC/H.265, and AV1 on TV chipsets (Amlogic, MediaTek, Qualcomm, Amazon Fire TV Silicons).
3. **Low Latency & Live Edge Buffer Control**: Granular buffer controls (`DefaultLoadControl`) required to handle intermittent IPTV connection drops, reconnection backoff, and live edge catchup.
4. **Permissive Licensing**: AndroidX Media3 is distributed under the **Apache-2.0 License**, guaranteeing full commercial freedom with zero GPL copyleft risks.
5. **Clean Separation of Concerns**: Multi-module architecture ensures core domain models and parsing algorithms are pure Kotlin (`jvm`/`multiplatform` compatible) and isolated from platform UI drivers.

---

## 2. Multi-Module Project Structure

The project is structured into modular Gradle subprojects to keep concerns decoupled, easily testable, and extensible for future SKYRAS Media features:

```
skyras-tv/
├── core-model/                 # Pure Kotlin domain data classes (Channel, EpgProgram, VodItem, SeriesItem)
├── core-iptv/                  # M3U/M3U8 parser, XMLTV parser, and in-memory PlaylistRepository
├── core-xtream/                # Xtream Codes API client (Live, VOD, Series, Player API authentication)
├── core-prefs/                 # Mock sign-in + UserPreferencesRepository (local + optional Supabase REST)
├── feature-ai-assist/          # AI Assist Engine, Pluggable LlmProvider, MockLlmProvider & OpenAI-compatible client
├── feature-sports-picks/       # Affiliate stub module (DraftKings/FanDuel referral links, feature-flagged, geo/age-gate TODO)
├── feature-community/          # Community slot placeholder (nav entry, route, configurable deep-link URL, feature-flagged)
├── app-tv/                     # TV Focus Controller, D-Pad state machine, and Media3 player abstractions
└── web-preview/                # Live companion application serving TV remote emulator, player OSD, and local mock API
```

---

## 3. Data Flow & Subsystem Interaction

### A0. Sign-in and preference persistence
First launch shows a single TV sign-in screen. `MockCredentialAuth` accepts username + password (length ≥ 3) and an optional M3U URL. There is no production identity provider. On success, `SessionStore` keeps the username locally and `UserPreferencesRepository` writes `user_preferences` to SharedPreferences, plus Supabase PostgREST when `BuildConfig` is not using placeholders.

### A. Playlist & Xtream Codes Ingestion Flow
```
[ Sign-in (username / password / optional M3U URL) ]
                   │
                   ▼
     ┌───────────────────────────┐
     │   XtreamClient / M3uParser │
     └─────────────┬─────────────┘
                   │ Emits List<Channel>, List<Category>, List<VodItem>
                   ▼
     ┌───────────────────────────┐
     │    PlaylistRepository     │ ◄─── Loads XMLTV EPG data
     └─────────────┬─────────────┘
                   │ StateFlow<List<Channel>>, StateFlow<Map<ChannelId, EpgProgram>>
                   ▼
     ┌───────────────────────────┐
     │    TvFocusController      │
     └─────────────┬─────────────┘
                   │ Select Channel (D-Pad Enter)
                   ▼
     ┌───────────────────────────┐
     │     SkyrasPlayerCore      │ (Media3 / ExoPlayer Engine)
     └───────────────────────────┘
```

### B. AI Assist Panel Workflow (Feature 1)
1. **User Prompt Ingestion**: The viewer asks a natural-language query via TV remote or on-screen prompt (e.g., *"find me the game"*, *"what's on right now?"*, *"recommend comedies"*).
2. **Context Enrichment**: `AiAssistEngine` extracts key sports/media entities, queries `PlaylistRepository` for matching channels and currently airing EPG programs, and formats a concise channel manifest.
3. **Pluggable LLM Resolution**: Dispatches to the configured `LlmProvider`:
   - `MockLlmProvider`: Instant offline rule-based recommendations for hermetic testing and offline TV setups.
   - `OpenAiCompatibleProvider`: Configurable endpoint (OpenAI, Ollama, Groq, vLLM, LocalAI) using standard JSON payloads without hardcoded API keys.
4. **Action Dispatch**: Returns structured responses with one-click TV action triggers (`"Watch SKYRAS Sports 1 HD"`).

### C. Sports Picks / Bet Slot Workflow (Feature 2)
1. **Compliance by Design**: **Zero real-money wagering logic is permitted in SKYRAS TV.**
2. **Feature Flag**: Controlled via `SportsPicksConfig.isEnabled` (default: `false`).
3. **Affiliate Link-Out**: Generates affiliate referral deep-links to external regulated sportsbooks (e.g. DraftKings, FanDuel).
4. **Mandatory Production Safeguards**: Includes explicit code gates and `TODO`s for IP-based geolocation gating (GeoComply) and 21+ age verification modals before live traffic can be redirected.

### D. Community Slot Workflow (Spec Update)
1. **Modular Slot**: Controlled via `CommunityConfig.isEnabled` (feature-flagged).
2. **Configurable Destination**: Configurable URL (defaults to `https://discord.gg/skyras`) displayed with QR code helper text.
3. **Extensibility**: Ready for native in-app chat or deep-linking to Discord, Telegram, or SKYRAS Media forums.

---

## 4. Viewer-First Cable TV UI Hierarchy (TiviMate-Grade Experience)

Rather than resembling a multi-window web dashboard, SKYRAS TV employs a **viewer-first 10-foot television interface**:
1. **Fullscreen Video Dominance**: Live video fills the screen from initial cold launch. Video playback never halts during menu or guide navigation.
2. **Overlay Channel List (OK / Select)**: Pressing Enter/OK brings up a left-anchored translucent channel panel showing channel numbers, logos, now-playing titles, and progress bars directly over the running broadcast.
3. **Cable-Box Channel Zapping**:
   - Number pad direct tuning (`0-9`) with top-right OSD channel banner.
   - Channel Up (`▲ CH+`) and Channel Down (`▼ CH-`) zapping with instant tune-in.
   - Previous Channel recall (`L / PREV`) for fast back-and-forth toggling between two live events.
4. **Automated Info Banner**: Appears on every channel change at the bottom screen edge displaying now/next airings, times, and playback progress, automatically auto-hiding after 4.5 seconds.
5. **Centerpiece EPG Grid Guide with PIP Live Preview**:
   - Fullscreen electronic program guide with small live video preview in the top-left corner.
   - Program detail strip alongside the PIP preview showing comprehensive metadata.
   - 4-column timeline matrix across time and channel rows down.
6. **Secondary Drawer for Modular Features**:
   - Menu key (`M`) pulls in a slim side rail for secondary items (Settings, Sports Picks, Community).
   - AI Assist panel operates as a non-intrusive floating modal on the right edge over live video.

---

## 5. Security, Clean-Room & Licensing Verification

- **100% Original Implementation**: All parser logic, D-pad traversal state machines, Xtream DTOs, and AI reasoning algorithms were written cleanly from scratch. No reverse-engineered or decompiled code from TVMate or other proprietary applications was used.
- **Dependency Hygiene**: Only Apache-2.0 and MIT licensed open-source libraries are used (Kotlin stdlib, kotlinx-coroutines, kotlinx-serialization, OkHttp, JUnit 5). Full inventory documented in `LICENSES.md`.
- **Zero Real-Money Wagering**: The application contains no betting calculators, odds engines, or payment processors.
- **Privacy & Secrets**: No API keys or credentials are hardcoded into the source tree. All endpoints and credentials are user-configured at runtime.
