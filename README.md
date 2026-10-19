# Conflict Tracker

An Android app that puts global conflict events on a map next to country-level indicators, so you can see what is happening where — and in what economic and political context — without cross-referencing three different sites.

Built with Kotlin, Retrofit and the Mapbox Maps SDK.

## What it does

- **Maps ACLED conflict events** as severity-coded markers, with a legend for severity bands and an event detail view per marker.
- **Joins World Bank country indicators** to the countries you are looking at, so an event is read alongside the context it happened in rather than in isolation.
- **Filters by region, severity, event type and timeframe**, with an escalating-conflicts view that surfaces the places getting worse rather than just the places that are busy.
- **Search and watchlists** for tracking specific countries or regions over time.

## Architecture

```
app/src/main/java/com/example/conflicttrackerandroidapp/
├── api/
│   ├── AcledApiService.kt         Retrofit interface — conflict events
│   ├── WorldBankApiService.kt     Retrofit interface — country indicators
│   ├── ConflictRepository.kt      Fetches and combines both sources
│   ├── WorldBankRepository.kt
│   └── Models.kt                  Data classes / DTOs
├── MainActivity.kt                Map, filters, search
├── ConflictDetailActivity.kt      Single-event detail
├── EscalatingConflictActivity.kt  Region-level trend view
├── ConflictAdapter.kt             Event list rendering
├── RegionalConflictsAdapter.kt
└── FilterUtils.kt                 Shared filter predicates
```

The two feeds are kept behind separate services and repositories so the map layer never talks to a network client directly, and a feed can be swapped or stubbed without touching UI code.

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin |
| UI | AndroidX, AppCompat, Material Components, ConstraintLayout |
| State & async | `ViewModel` (lifecycle-viewmodel-ktx) + Kotlin coroutines |
| Networking | Retrofit 2.9 with the Gson converter |
| Mapping | Mapbox Maps SDK for Android 11.9.1 |
| Model passing | Kotlin Parcelize |
| Build | Gradle Kotlin DSL, AGP with `compileSdk` 34 / `targetSdk` 35 |

## Data sources

- **[ACLED](https://acleddata.com/)** — Armed Conflict Location & Event Data project.
- **[World Bank Open Data](https://data.worldbank.org/)** — country indicators.

Both are third-party services with their own terms of use and attribution requirements. This is an independent client and is not affiliated with either project.

## Running it

1. Open the project in Android Studio.
2. Supply your own **Mapbox access token** — the repository does not include one. Create a free token at [account.mapbox.com](https://account.mapbox.com/access-tokens/) and expose it to the Maps SDK per Mapbox's Android installation guide.
3. Build and run on a device or emulator running **Android 14 (API 34) or newer**.

> **Note:** `minSdk` is currently 34, so the app will not install on anything below Android 14. Lowering it is a sensible next step if you want this running on a wider range of devices.

## Status

A personal project from early 2025, published for reference rather than as a maintained product. The code is readable and reasonably structured, not production-hardened. Issues and pull requests are welcome but are not on a schedule.

## License

MIT — see [LICENSE](LICENSE).
