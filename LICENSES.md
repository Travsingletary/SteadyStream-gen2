# Open Source Dependency & License Inventory

**Project**: SKYRAS TV  
**Organization**: SKYRAS Media (Owner: Travis Singletary)  

All third-party libraries integrated into SKYRAS TV are evaluated to ensure they use permissive open-source licenses (Apache-2.0, MIT, BSD) compatible with commercial distribution. **No GPL or AGPL copyleft libraries are included.**

| Component / Library | Artifact / Coordinate | Version | License | Justification & Usage |
| :--- | :--- | :--- | :--- | :--- |
| **Kotlin Standard Library** | `org.jetbrains.kotlin:kotlin-stdlib` | 2.0.21 | Apache-2.0 | Core programming language runtime. |
| **Kotlin Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.9.0 | Apache-2.0 | Asynchronous flow and background network thread scheduling. |
| **Kotlin Serialization** | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.3 | Apache-2.0 | Fast, zero-reflection JSON encoding for Xtream and LLM APIs. |
| **Kotlin Date/Time** | `org.jetbrains.kotlinx:kotlinx-datetime` | 0.6.1 | Apache-2.0 | Modern ISO & epoch timestamp utilities for EPG guides. |
| **OkHttp** | `com.squareup.okhttp3:okhttp` | 4.12.0 | Apache-2.0 | HTTP client for Xtream Codes and LLM endpoint communication. |
| **MockWebServer** | `com.squareup.okhttp3:mockwebserver` | 4.12.0 | Apache-2.0 | Test-only local HTTP server for hermetic unit testing. |
| **JUnit 5 Jupiter** | `org.junit.jupiter:junit-jupiter` | 5.11.2 | EPL-2.0 / Apache-2.0 | Industry standard test runner for multi-module Kotlin test suites. |
| **AndroidX Media3 / ExoPlayer** | `androidx.media3:media3-exoplayer` | 1.4.1+ (Target) | Apache-2.0 | Target core playback engine for HLS, DASH, and TS streams on Android TV / Fire TV. |
| **Tailwind CSS** | Standalone CDN bundle | 3.x | MIT | Permissive styling framework for web companion and remote emulator. |
| **Hls.js** | Standalone CDN bundle | 1.x | Apache-2.0 | HLS stream playback engine for companion web preview. |

---

## Clean-Room Statement
- No decompiled or proprietary code from TVMate, TiviMate, or any commercial IPTV application was consulted, copied, or utilized.
- All M3U/M3U8 parsing, XMLTV date parsing, Xtream Codes serialization, AI query keyword matching, and TV focus handling algorithms were written 100% from scratch by clean-room principles.
