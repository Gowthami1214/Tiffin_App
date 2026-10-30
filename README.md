# 🍱 Tiffin App

A single-module Android app displaying home-cooked meal subscriptions from local home kitchens.
Built for the Propel Spark Android Developer Intern assignment — Task 1.

---

## Architecture

**MVVM + Jetpack Compose (Material 3)**

```
MainActivity
  └── TiffinAppTheme
        └── AppNavigation (NavHost)
              ├── KitchenListScreen   ← driven by TiffinViewModel + KitchenRepository
              ├── KitchenDetailScreen ← reads selectedKitchen from ViewModel
              └── PaywallScreen       ← shown on 3rd launch OR any subscribe tap
```

- **Data**: Bundled `assets/kitchens.json` → `KitchenRepository` → cold `Flow<KitchenState>`
- **State**: `TiffinViewModel` exposes `StateFlow`s consumed by all screens
- **Persistence**: `DataStore Preferences` — `launch_count` (Int) + `is_paid` (Boolean)
- **Navigation**: Navigation Compose with a `Screen` sealed class; paywall injected via `LaunchedEffect`

---

## Screens

### 1. Kitchen List
- Top bar with "Tiffin" branding + subtitle + visual search bar placeholder
- `LazyColumn` of kitchen cards: name, cuisine, price/tiffin, veg/non-veg tag, ★ rating badge
- Orange "Subscribe" button per card
- Shimmer loading state, illustrated empty state, error state with Retry

### 2. Kitchen Detail
- Hero gradient banner, info chips (rating / veg / price)
- Full Monday–Sunday weekly menu in card rows
- Sticky bottom CTA: "Subscribe — ₹X / tiffin"

### 3. Paywall
- Triggered automatically on the **3rd app launch** or on any **Subscribe** tap (if not yet paid)
- **Never shown again** once `is_paid = true` is persisted in DataStore
- Dynamic copy: *"₹1 is charged now, and ₹249 will be charged on [Day, DD MMM YYYY] 24 hours later."* — date computed at runtime as `now + 86_400_000ms`
- "Start Trial & Subscribe" → writes `is_paid = true` to DataStore, dismisses paywall

---

## Analytics — 4 Instrumented Moments

| Event | Where | Why I picked it |
|---|---|---|
| `kitchen_list_viewed` | On list screen entry | Top-of-funnel engagement |
| `kitchen_card_clicked` | On card tap | Intent signal before detail |
| `kitchen_detail_viewed` | On detail screen entry | Deeper intent |
| `paywall_shown` | When paywall opens | Conversion funnel start (with trigger: launch_count or subscribe_button) |
| `subscription_completed` | On "Start Trial" tap | Conversion event |

> Implementation: `AnalyticsTracker` interface + `LogcatAnalyticsTracker` (logs to `D/Analytics`).
> Swap in Firebase/Amplitude by replacing the implementation — call sites unchanged.

---

## Persistence Guarantees

| Scenario | Behaviour |
|---|---|
| Screen rotation | DataStore + `StateFlow` — state is preserved, no re-fetch |
| Process death (force stop) | DataStore survives — `is_paid` and `launch_count` restored on next launch |
| App restart (3rd launch) | `launchCount` incremented in ViewModel `init`, paywall shown automatically |
| Already paid, taps Subscribe | `onSubscribeClicked()` checks `isPaid` first — paywall silently skipped |

---

## What I Cut and Why

Nothing was deliberately cut from the core spec. All three screens, all UI states, DataStore persistence, and the `analytics.kt` interface are present and working.

**Conscious simplifications** (not cuts):
- **No image loading library** (Coil/Glide): The spec says "placeholder" for kitchen images. I used a 🍱 emoji hero block — avoids a 500 KB+ dependency for a demo asset.
- **No DI framework** (Hilt/Koin): Single-module app with one ViewModel — constructor injection via `AndroidViewModel(application)` is simpler and equally correct for this scope.
- **No search functionality**: The search bar is a visual placeholder as specified ("visual search bar placeholder").
- **No unit tests**: The scope note says 150 min — tests were the first cut. The architecture (repository + ViewModel with injected dependencies) is fully testable.

---

## Tech Stack

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2026.02.01 | UI toolkit |
| Material 3 | BOM-managed | Design system |
| Navigation Compose | 2.8.9 | Screen routing |
| DataStore Preferences | 1.1.7 | Persistent storage |
| kotlinx-serialization-json | 1.7.3 | JSON parsing |
| Lifecycle ViewModel Compose | 2.11.0 | MVVM |
| Material Icons Extended | BOM-managed | UI icons |

**Min SDK**: 24 | **Target SDK**: 37 | **Language**: Kotlin 2.2.10

---

## Running the App

```bash
# Clone and open in Android Studio, then:
./gradlew assembleDebug
# or just press Run ▶ in Android Studio
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
