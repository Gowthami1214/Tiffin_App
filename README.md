# 🍱 Tiffin App

A single-module Android app displaying home-cooked meal subscriptions from local home kitchens.
Built for the Propel Spark Android Developer Intern assignment.

---

## ✂️ What I Cut and Why (Cut List)

Nothing was cut from the core specification: all three screens, every explicit UI state (loading shimmer, empty illustration, error with retry), DataStore persistence across process death, and the `analytics.kt` logging system were fully implemented.

**Deliberate, conscious cuts & simplifications:**
1. **Third-party image loading library (Coil/Glide):** The prompt specified an `imageResId`/`placeholder`. Instead of importing a 500 KB+ dependency for mock data, each kitchen uses an styled 🍱 hero banner with Material 3 color palettes.
2. **Dependency Injection Framework (Hilt/Koin):** In a single-module Compose app with one shared ViewModel, constructor injection through `AndroidViewModel(application)` is clean, zero-overhead, and keeps build times sub-30 seconds.
3. **Interactive Search Filter:** Kept as a styled visual placeholder per specification to prioritize paywall flow, monetization persistence, and robust edge-to-edge insets.
4. **Unit & Instrumented UI Tests:** Given the 150-minute deadline, manual verification and end-to-end ADB logcat checks were prioritized over writing test suites.

---

## 🛠️ Task 2 — Fix What the Agent Wrote

### The 4 Problems

1. **Hardcoded Secret API Key in Client Code (`"sk_live_9f2c41ab"`) — *Gets us a bill***  
   * **What breaks:** Anyone can decompile the APK or inspect network traffic, extract the live secret key, drain API quotas, or rack up massive unauthorized charges. Secret keys must never live in client code; they belong on a secure backend server.
2. **Synchronous Network Call on the Main Thread (`URL.readText()`) — *Gets us a one-star review***  
   * **What breaks:** `LaunchedEffect` executes on `Dispatchers.Main` by default. Performing blocking network I/O on the main thread throws a `NetworkOnMainThreadException` and crashes the app immediately on launch.
3. **Unobserved Non-Compose State (`mutableListOf<Kitchen>()`)**  
   * **What breaks:** A plain `mutableListOf` is neither `remember`ed nor an observable Compose state. Mutating it via `.addAll()` never triggers recomposition—the screen stays blank forever, and state is lost on rotation.
4. **Bypassing the ViewModel & Business Logic in the UI Layer**  
   * **What breaks:** `vm: KitchenViewModel` is received as a parameter but completely bypassed. Fetching, parsing, and storing data directly inside the Composable violates MVVM, restarts network calls on recomposition/rotation, and lacks loading/error state handling.

### Corrected Code

#### 1. ViewModel & UI State
```kotlin
sealed interface KitchenUiState {
    data object Loading : KitchenUiState
    data class Success(val kitchens: List<Kitchen>) : KitchenUiState
    data class Error(val message: String) : KitchenUiState
}

class KitchenViewModel(
    private val repository: KitchenRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<KitchenUiState>(KitchenUiState.Loading)
    val uiState: StateFlow<KitchenUiState> = _uiState.asStateFlow()

    init {
        loadKitchens()
    }

    fun loadKitchens() {
        viewModelScope.launch {
            _uiState.value = KitchenUiState.Loading
            try {
                // Network call occurs on IO dispatcher through repository (client public key or backend proxy)
                val data = repository.getKitchens()
                _uiState.value = KitchenUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = KitchenUiState.Error(e.localizedMessage ?: "Failed to load")
            }
        }
    }
}
```

#### 2. Clean Composable
```kotlin
@Composable
fun KitchenList(
    vm: KitchenViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is KitchenUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is KitchenUiState.Success -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                items(
                    items = state.kitchens,
                    key = { it.id } // Stable keys prevent unnecessary redraws
                ) { kitchen ->
                    KitchenRow(kitchen)
                }
            }
        }
        is KitchenUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = vm::loadKitchens) {
                    Text("Retry")
                }
            }
        }
    }
}
```

---

## 🚀 Task 3 — The Release Note

### 1. Manual Pre-Publish Test Plan & Devices
* **Target Devices:**
  * **Low-end / Min SDK 24:** *Redmi 9A / Galaxy A03 (Android 8/9, 2GB RAM)* — tests low-memory process death, slow disk I/O with DataStore, and scroll performance on low-tier GPUs.
  * **High-end / Edge-to-Edge:** *Pixel 8/9 (Android 15)* — verifies `navigationBarsPadding()` across gesture navigation bars and display cutouts.
  * **Accessibility / Dense Display:** *Device with Display Size & Font Size set to 1.3x* — guarantees the Paywall CTA and dynamic charge text don't clip off-screen.
* **Non-Negotiable Manual Checks:**
  * **Launch Gate:** Fresh install $\rightarrow$ Kill and relaunch $3\times$. Verify Paywall opens *only* on launch 3.
  * **Process Death:** Enable *"Don't keep activities"* in Dev Options. Open Detail screen, background app, return — verify no crash and state intact.
  * **Monetization Persistence:** Tap *"Start Trial"* $\rightarrow$ Force-stop app in Settings $\rightarrow$ Relaunch $\rightarrow$ Verify Paywall never shows again on cold start or Subscribe click.

### 2. What Will Break First & Observability
* **The Failure:** **Date & Locale formatting / Time desynchronization on the Paywall.**  
  Using system time offsets (`System.currentTimeMillis() + 24h`) paired with local formatting will produce malformed text or crash on devices set to non-Gregorian calendars (e.g., Buddhist/Islamic calendar locales) or manual clock overrides.
* **How We Catch It Without User Reports:**
  * **Crashlytics Velocity Alert:** Automated PagerDuty/Slack alert triggered if crash-free sessions dip below **99.5%** or if `DateTimeParseException` / DataStore `IOException` spikes.
  * **Funnel Drop-Off Metric (Mixpanel / BigQuery):** Track the ratio of `paywall_shown` $\rightarrow$ `subscription_completed`. If the conversion rate drops by $>40\%$ relative to baseline in a 1-hour rolling window, an alert fires.

### 3. 11:00 PM Rollback Strategy
On Android, you cannot instantly roll back an installed client APK.
1. **Immediate Circuit Breaker (< 5 mins):** If the paywall is gated via Firebase Remote Config, toggle `show_paywall_enabled = false` remotely. Existing apps fetch updated values on restart, bypassing the broken gate with zero Play Store delay.
2. **Play Console Halt:** Immediately halt staged rollout (if at 10%–20%) to prevent further user exposure.
3. **Emergency Hotfix Release:** Cherry-pick the fix onto the release tag, increment `versionCode`, and trigger automated CI to produce a signed release AAB. Submit to the Play Store production track with an **Expedited Review** request.

### 4. The Next 3 Features to Build (In Order)
1. **Real Payment Gateway Integration (Razorpay / UPI Autopay):** Replace mock persistence with UPI recurring mandates and server-side webhook verification for reliable recurring tiffin billing.
2. **Subscription & Meal Calendar Controls:** Allow users to pause deliveries (e.g., skip weekend/vacation days), switch between lunch and dinner slots, and cancel trials prior to day 2 renewal.
3. **Delivery Address & Real-time Order Tracking:** Address autofill via Google Places SDK, local kitchen geofencing (delivery within 5km radius), and daily tiffin dispatch status.

---

## 🤖 Task 4 — How I Used AI

### 1. Tools Used and Share of Code
* **Tools:** Google Antigravity Agentic Assistant / Gemini & Claude LLMs inside Android Studio, coupled with ADB and UIAutomator for runtime inspection.
* **Share of Code:** Roughly **75% AI-generated** (initial data layer boilerplate, 12-kitchen mock JSON schema, basic screen layouts, and ViewModel skeletons) and **25% hand-crafted / manually debugged** (StateFlow sharing policies, Edge-to-Edge system insets, Gradle Version Catalog dependency resolution, and dynamic button state switches).

### 2. The Single Best Prompt (Pasted Exactly)
```text
Act as a senior Android developer. I need to build a single-module Android app called "Tiffin" using Kotlin and Jetpack Compose (Material 3, Min SDK 24). 

The app displays home-cooked meal subscriptions from local home kitchens.

Here is the exact technical specification:

1. ARCHITECTURE & DEPENDENCIES
- Single-module Compose app using MVVM architecture with StateFlow and Coroutines.
- Use Navigation Compose for screen routing.
- Use DataStore Preferences to persist launch count and subscription/paid status across process death, rotations, and app restarts.
- Include a simple Analytics Tracker interface in `analytics.kt`:
  interface AnalyticsTracker { fun logEvent(eventName: String, params: Map<String, Any> = emptyMap()) }
  along with a `LogcatAnalyticsTracker` implementation that logs each event using `android.util.Log.d("Analytics", ...)`.

2. LOCAL DATA LAYER
- Provide a bundled JSON string asset format (representing `assets/kitchens.json`) containing ~12 mock home kitchens.
- Each kitchen must have: `id`, `name`, `cuisineType` (e.g., Punjabi, South Indian), `pricePerTiffin` (Int, INR), `isVeg` (Boolean), `rating` (Double), `imageResId`/`placeholder`, and `weeklyMenu` (Map<String, String> of Mon-Sun menus).
- Create a `KitchenRepository` that reads and deserializes this JSON. It must expose states via Kotlin Flow: Loading, Success(List<Kitchen>), Empty, and Error (with a retry function).

3. SCREENS TO IMPLEMENT
A. Kitchen List Screen:
- Top bar with "Tiffin" branding, subtitle "Home-cooked meals delivered daily", and a visual search bar placeholder.
- Vertical LazyColumn displaying kitchen cards matching:
  * Name, cuisine, price (e.g., "₹120 / tiffin"), veg/non-veg tag, and rating badge (e.g., "★ 4.8").
  * Orange "Subscribe" button on each card.
  * Explicit UI states: loading spinner/shimmer, empty state illustration/text, and error state with retry.
- Clicking a card navigates to Detail. Clicking "Subscribe" triggers the Paywall.
- Log analytics event: "kitchen_card_clicked" and "kitchen_list_viewed".

B. Kitchen Detail Screen:
- Displays kitchen details, cuisine, rating, price, and full weekly menu (Monday to Sunday).
- Large sticky or bottom CTA: "Subscribe".
- Log analytics event: "kitchen_detail_viewed".

C. Paywall Screen & Monetization Flow:
- Triggered automatically on the THIRD app launch, or immediately when clicking any "Subscribe" button.
- It must NEVER show again once the user has completed a purchase.
- Copy Requirement: Must explicitly display dynamically computed text:
  "₹1 is charged now, and ₹249 will be charged on [Compute Name of Day and Date 24 hours from current time] 24 hours later."
- CTA button "Start Trial & Subscribe": clicking it marks the subscription state as `paid = true` in DataStore and dismisses the paywall.
- Log analytics event: "paywall_shown" (with trigger: "launch_count" or "subscribe_button") and "subscription_completed".

Please generate: arctiecture and work flow as well
1. Data models and JSON parser repository.
2. ViewModel handling UI states, app launch tracking (incrementing on launch), and DataStore persistence.
3. The NavHost and composables for List, Detail, and Paywall screens.
4. `analytics.kt` and its instrumentation points.
Write clean, production-ready Kotlin code adhering strictly to Compose best practices.
```

### 3. What the Agent Got Wrong in My Actual Build & How I Caught It
* **The Bug:** `StateFlow` Cold Subscription Failure in `TiffinViewModel`.  
  The agent wrote:  
  ```kotlin
  val isPaid: StateFlow<Boolean> = prefs.isPaidFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = false
  )
  ```
  Because the UI only read `isPaid.value` imperatively inside button click callbacks (`if (!isPaid.value)`) without an active Compose observer (`collectAsState()`), the flow had zero subscribers. As a result, `prefs.isPaidFlow` never started collecting, and `isPaid.value` remained permanently stuck at `false`.
* **How I Caught It:**  
  I ran an end-to-end verification via `adb logcat -s Analytics`. After completing the ₹1 trial purchase, tapping "Subscribe" on a kitchen card still triggered `paywall_shown`.
* **What I Did:**  
  Changed the sharing strategy from `SharingStarted.WhileSubscribed(5_000)` to `SharingStarted.Eagerly`. This forced the ViewModel to start listening to DataStore immediately upon instantiation, correctly updating `isPaid.value` across all imperative checks and process restarts.

### 4. What I Decided to Write / Fix by Hand Because Directing the Agent Was Slower
* **My Workflow:**  
  Before jumping into coding, I broke down the problem statement into distinct layers: architecture, data flow, screen routes, and persistence boundaries. I refined the overarching prompt to output architecture and workflow documentation first. Once the skeleton was established, I built module by module, verifying each feature's functionality before moving to the next.
* **What I Handled Manually:**  
  * **Dynamic Button State & Feedback:** The agent initially left the "Subscribe" buttons static. After purchase, tapping them simply did nothing because the paywall was muted. Rather than repeatedly prompting the agent on UX nuances, I directly wired `isPaid` into `KitchenCard` and `KitchenDetailScreen` to toggle the buttons to **"Subscribed ✓"** in green with custom Toast feedback.
  * **Edge-to-Edge System Navigation Insets:** On Android 15 / modern gesture nav, the agent's bottom CTA was clipped behind the navigation pill. It was much faster to directly inspect the layout in Android Studio and add `.navigationBarsPadding()` to the specific bottom surfaces than iterating in natural language prompts.

---

## 🏛️ Architecture & Overview (Task 1)

**MVVM + Jetpack Compose (Material 3)**

```
MainActivity
  └── TiffinAppTheme
        └── AppNavigation (NavHost)
              ├── KitchenListScreen   ← driven by TiffinViewModel + KitchenRepository
              ├── KitchenDetailScreen ← reads selectedKitchen from ViewModel
              └── PaywallScreen       ← shown on 3rd launch OR any subscribe tap
```

- **Data**: Bundled `assets/kitchens.json` $\rightarrow$ `KitchenRepository` $\rightarrow$ cold `Flow<KitchenState>`
- **State**: `TiffinViewModel` exposes `StateFlow`s consumed by all screens
- **Persistence**: `DataStore Preferences` — `launch_count` (Int) + `is_paid` (Boolean)
- **Navigation**: Navigation Compose with a `Screen` sealed class; paywall injected via `LaunchedEffect`

---

## 📱 Screens Summary

### 1. Kitchen List
- Top bar with "Tiffin" branding + subtitle + visual search bar placeholder
- `LazyColumn` of kitchen cards: name, cuisine, price/tiffin, veg/non-veg tag, ★ rating badge
- Dynamic CTA: "Subscribe" (orange) switches to "Subscribed ✓" (green) upon plan activation
- Shimmer loading state, illustrated empty state, error state with Retry

### 2. Kitchen Detail
- Hero gradient banner, info chips (rating / veg / price)
- Full Monday–Sunday weekly menu in card rows
- Sticky bottom CTA: "Subscribe — ₹X / tiffin" (orange) switches to "✓ Plan Active — Daily Delivery Confirmed" (green)

### 3. Paywall
- Triggered automatically on the **3rd app launch** or on any **Subscribe** tap (if not yet paid)
- **Never shown again** once `is_paid = true` is persisted in DataStore
- Dynamic copy: *"₹1 is charged now, and ₹249 will be charged on [Day, DD MMM YYYY] 24 hours later."* — date computed dynamically at runtime (`now + 24 hours`)
- "Start Trial & Subscribe" $\rightarrow$ writes `is_paid = true` to DataStore, dismisses paywall

---

## 📡 Analytics — 4 Instrumented Moments

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

## 💾 Persistence Guarantees

| Scenario | Behaviour |
|---|---|
| Screen rotation | DataStore + `StateFlow` — state is preserved, no re-fetch |
| Process death (force stop) | DataStore survives — `is_paid` and `launch_count` restored on next launch |
| App restart (3rd launch) | `launchCount` incremented in ViewModel `init`, paywall shown automatically |
| Already paid, taps Subscribe | `onSubscribeClicked()` checks `isPaid` first — paywall silently skipped |

---

## 📦 Tech Stack

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

## 🏃 Running the App

```bash
# Clone and open in Android Studio, then:
./gradlew assembleDebug
# or press Run ▶ in Android Studio
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`
