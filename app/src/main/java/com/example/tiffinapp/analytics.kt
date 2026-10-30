package com.example.tiffinapp

import android.util.Log

/**
 * Analytics abstraction. Swap [LogcatAnalyticsTracker] with a real implementation
 * (Firebase, Amplitude, etc.) without touching call-sites.
 */
interface AnalyticsTracker {
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap())
}

/** Development implementation — prints to Logcat under the "Analytics" tag. */
class LogcatAnalyticsTracker : AnalyticsTracker {
    override fun logEvent(eventName: String, params: Map<String, Any>) {
        val paramsStr = if (params.isEmpty()) "" else " | params=$params"
        Log.d("Analytics", "EVENT: $eventName$paramsStr")
    }
}

/** Singleton holder so the tracker can be injected without a DI framework. */
object Analytics {
    val tracker: AnalyticsTracker = LogcatAnalyticsTracker()

    // ── Event constants ──────────────────────────────────────────────────────
    const val EVENT_KITCHEN_LIST_VIEWED   = "kitchen_list_viewed"
    const val EVENT_KITCHEN_CARD_CLICKED  = "kitchen_card_clicked"
    const val EVENT_KITCHEN_DETAIL_VIEWED = "kitchen_detail_viewed"
    const val EVENT_PAYWALL_SHOWN         = "paywall_shown"
    const val EVENT_SUBSCRIPTION_COMPLETED = "subscription_completed"

    // ── Param keys ───────────────────────────────────────────────────────────
    const val PARAM_KITCHEN_ID   = "kitchen_id"
    const val PARAM_KITCHEN_NAME = "kitchen_name"
    const val PARAM_TRIGGER      = "trigger"

    // ── Trigger values ───────────────────────────────────────────────────────
    const val TRIGGER_LAUNCH_COUNT     = "launch_count"
    const val TRIGGER_SUBSCRIBE_BUTTON = "subscribe_button"
}
