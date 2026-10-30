package com.example.tiffinapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tiffinapp.Analytics
import com.example.tiffinapp.data.model.Kitchen
import com.example.tiffinapp.data.model.KitchenState
import com.example.tiffinapp.data.repository.KitchenRepository
import com.example.tiffinapp.datastore.AppPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the entire app's UI state.
 *
 * Responsibilities:
 * - Loads kitchen data via [KitchenRepository].
 * - Tracks and persists app launch count via [AppPreferences].
 * - Determines when to show the paywall (3rd launch or subscribe click).
 * - Exposes [UiState] to composables via [StateFlow].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TiffinViewModel(application: Application) : AndroidViewModel(application) {

    private val repository   = KitchenRepository(application)
    private val prefs        = AppPreferences(application)
    private val analytics    = Analytics.tracker

    // ── Retry trigger ────────────────────────────────────────────────────────
    private val _retryTrigger = MutableStateFlow(0)

    // ── Kitchen list state ───────────────────────────────────────────────────
    val kitchenState: StateFlow<KitchenState> = _retryTrigger
        .flatMapLatest { repository.getKitchens() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = KitchenState.Loading
        )

    // ── Paywall visibility ───────────────────────────────────────────────────
    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    /** Paywall trigger source passed to the paywall screen for analytics. */
    private val _paywallTrigger = MutableStateFlow(Analytics.TRIGGER_LAUNCH_COUNT)
    val paywallTrigger: StateFlow<String> = _paywallTrigger.asStateFlow()

    // ── Subscription / launch state (combined for convenience) ───────────────
    val isPaid: StateFlow<Boolean> = prefs.isPaidFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val launchCount: StateFlow<Int> = prefs.launchCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0
        )

    // ── Selected kitchen for detail screen ───────────────────────────────────
    private val _selectedKitchen = MutableStateFlow<Kitchen?>(null)
    val selectedKitchen: StateFlow<Kitchen?> = _selectedKitchen.asStateFlow()

    // ────────────────────────────────────────────────────────────────────────
    // Init — increment launch count, check paywall condition
    // ────────────────────────────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            prefs.incrementLaunchCount()

            // Observe combined state: if this is the 3rd+ launch and user is not paid → show paywall
            combine(prefs.launchCountFlow, prefs.isPaidFlow) { count, paid ->
                count to paid
            }.collect { (count, paid) ->
                if (count >= 3 && !paid && !_showPaywall.value) {
                    _paywallTrigger.value = Analytics.TRIGGER_LAUNCH_COUNT
                    _showPaywall.value = true
                    analytics.logEvent(
                        Analytics.EVENT_PAYWALL_SHOWN,
                        mapOf(Analytics.PARAM_TRIGGER to Analytics.TRIGGER_LAUNCH_COUNT)
                    )
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Public actions
    // ────────────────────────────────────────────────────────────────────────

    /** Retriggers the kitchen list flow (e.g. after an error). */
    fun retry() {
        _retryTrigger.update { it + 1 }
    }

    /** Called when the user taps a kitchen card. */
    fun onKitchenSelected(kitchen: Kitchen) {
        _selectedKitchen.value = kitchen
        analytics.logEvent(
            Analytics.EVENT_KITCHEN_CARD_CLICKED,
            mapOf(
                Analytics.PARAM_KITCHEN_ID   to kitchen.id,
                Analytics.PARAM_KITCHEN_NAME to kitchen.name
            )
        )
    }

    /**
     * Called when the user taps a "Subscribe" button.
     * Shows the paywall only if the user hasn't already subscribed.
     */
    fun onSubscribeClicked() {
        if (!isPaid.value) {
            _paywallTrigger.value = Analytics.TRIGGER_SUBSCRIBE_BUTTON
            _showPaywall.value = true
            analytics.logEvent(
                Analytics.EVENT_PAYWALL_SHOWN,
                mapOf(Analytics.PARAM_TRIGGER to Analytics.TRIGGER_SUBSCRIBE_BUTTON)
            )
        }
    }

    /** Marks the subscription as paid and dismisses the paywall. */
    fun completeSubscription() {
        viewModelScope.launch {
            prefs.markAsPaid()
            _showPaywall.value = false
            analytics.logEvent(Analytics.EVENT_SUBSCRIPTION_COMPLETED)
        }
    }

    /** Dismisses the paywall without subscribing (back press). */
    fun dismissPaywall() {
        _showPaywall.value = false
    }
}
