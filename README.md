# Agent Ayuemin — Android debug preview

**Development plan:** [ROADMAP.md](ROADMAP.md) — mandatory architecture rules, tasks, status, regression checklist. Always update when working on the code.

This repository contains an experimental, minimal Android chat client (version 0.1.0-debug).

## Build and APK

GitHub Actions: [Debug APK](https://github.com/Ayuemin/Agent-Ayuemin/actions/workflows/debug-apk.yml). The workflow builds `app-debug.apk`, uploads it as an Actions artifact, and **only after a successful build** publishes it under [Releases](https://github.com/Ayuemin/Agent-Ayuemin/releases).

This is an experimental debug APK. GitHub-hosted debug builds use ephemeral debug signing keys. Uninstalling an earlier debug build may be required to install a new build, which erases application-local data. Do **not** rely on this build for important conversation history.

## Implemented in the initial code

- Basic Android chat with multiple local conversations.
- Configurable OpenAI Chat Completions-compatible endpoint, API key and model ID.
- Local SQLite message history.
- Local **lexical retrieval** from old messages (SQLite FTS4). This is a first retrieval baseline, **not semantic RAG**.
- Last-N-turn context assembler and experimental summary-on-threshold.
- Ring indicator for estimated working-context usage.
- Per-chat aggregate token usage and estimated spending using manually configured rates.
- Text file import (up to 200 KB). Non-text files/images are **not yet supported**.
- UI text in Russian/English, selected using Android system language.

## Not implemented yet

- API embeddings and semantic vector retrieval, hybrid ranking and citation provenance.
- Reliable state compaction when the working window is persistently small; compaction quality regression testing.
- Native Anthropic/Gemini adapters; provider support currently **only OpenAI-compatible** APIs.
- Full image and PDF attachment handling, streaming responses.
- Stable signing, encrypted API key storage, plugin Tool Bus and production-ready UI.
- Accurate provider-specific pricing tiers and separate paid cache write billing.
- Tests on actual devices; this preview is not production ready.

## Security

API keys are kept in app-private Android SharedPreferences **without additional encryption** in this preliminary build. Do not use high-value keys. Network requests require HTTPS. The Android application has backups disabled.

## Architecture direction

Full, original message history remains in SQLite. The model request contains a bounded recent-message window plus locally retrieved older excerpts and optional task summary. A later revision must replace the lexical-only retrieval with API embeddings + hybrid RAG and isolate the components into independent modules.

## Build locally

Requires JDK 17, Android SDK 35 and Gradle 8.13:

```sh
gradle :app:assembleDebug
```
