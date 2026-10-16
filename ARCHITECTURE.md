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
├── feature-ai-assist/          # AI Assist Engine, Pluggable LlmProvider, MockLlmProvider & OpenAI-compatible client
├── feature-sports-picks/       # Affiliate stub module (DraftKings/FanDuel referral links, feature-flagged, geo/age-gate TODO)
├── feature-community/          # Community slot placeholder (nav entry, route, configurable deep-link URL, feature-flagged)
├── app-tv/                     # TV Focus Controller, D-Pad state machine, and Media3 player abstractions
└── web-preview/                # Live companion application serving TV remote emulator, player OSD, and local mock API
```

---

## 3. Data Flow & Subsystem Interaction

### A. Playlist & Xtream Codes Ingestion Flow
```
[ User Input (M3U URL/File or Xtream Creds) ]
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

## 4. Security, Clean-Room & Licensing Verification

- **100% Original Implementation**: All parser logic, D-pad traversal state machines, Xtream DTOs, and AI reasoning algorithms were written cleanly from scratch. No reverse-engineered or decompiled code from TVMate or other proprietary applications was used.
- **Dependency Hygiene**: Only Apache-2.0 and MIT licensed open-source libraries are used (Kotlin stdlib, kotlinx-coroutines, kotlinx-serialization, OkHttp, JUnit 5). Full inventory documented in `LICENSES.md`.
- **Zero Real-Money Wagering**: The application contains no betting calculators, odds engines, or payment processors.
- **Privacy & Secrets**: No API keys or credentials are hardcoded into the source tree. All endpoints and credentials are user-configured at runtime.
