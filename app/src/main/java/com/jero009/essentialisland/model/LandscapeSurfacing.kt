/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.model

import com.jero009.essentialisland.data.SmartIslandSettings

/**
 * Landscape card surfacing (Settings > Show notification cards in landscape, on with the island itself hidden).
 *
 * Two rules, both pure so they can be tested and shared by every window-visibility call site:
 *  1. Only app notifications and incoming calls wake the hidden island. Device-state cards (charging,
 *     Bluetooth, Wi-Fi, VPN, USB, headphones, hotspot, flashlight, timers, music, navigation, ...) must not
 *     surface anything in landscape; in portrait they behave exactly as before. The per-event landscape
 *     switches (disabledEventsLandscape) are still honoured: an app-notification or call event switched off
 *     for landscape does not wake the island either.
 *  2. When the last waking card leaves, the view stays up for a linger window so the island can shrink back
 *     into the pill's spot and fade out instead of being cut off mid-morph (the fade is the linger's tail).
 */
object LandscapeSurfacing {

    /** Base linger after the last waking card leaves: the collapse morph plus the fade tail. Scaled by the animation speed setting. */
    const val SHRINK_LINGER_BASE_MS = 900

    /** Base length of the fade-out tail at the end of the linger. Scaled by the animation speed setting. */
    const val FADE_TAIL_BASE_MS = 200

    /**
     * True when this card may wake / surface the hidden landscape island. App notifications and incoming
     * calls only, and only when their event's landscape switch is on.
     */
    fun wakesHiddenIsland(
        notification: IslandNotification,
        eventEnabledInLandscape: (String) -> Boolean
    ): Boolean {
        if (notification.mode != IslandMode.Notification && notification.mode != IslandMode.IncomingCall) return false
        val event = IslandEvent.forNotification(notification)
        return event == null || eventEnabledInLandscape(event.id)
    }

    /** How many of the shown cards may wake the hidden landscape island. */
    fun wakeCount(
        notifications: List<IslandNotification>,
        eventEnabledInLandscape: (String) -> Boolean
    ): Int = notifications.count { wakesHiddenIsland(it, eventEnabledInLandscape) }

    /**
     * True while the view must stay up although no waking card is left: the shrink-back morph is still inside
     * its linger window. Only applies to the hidden-island case (the visible landscape pill keeps its own
     * idle-circle hand-over), and only while the cards toggle itself is on.
     */
    fun viewStaysForShrink(
        isLandscape: Boolean,
        showInLandscape: Boolean,
        showNotificationCardsInLandscape: Boolean,
        wakeCount: Int,
        nowMs: Long,
        shrinkLingerUntilMs: Long
    ): Boolean =
        isLandscape && !showInLandscape && showNotificationCardsInLandscape &&
            wakeCount == 0 && nowMs < shrinkLingerUntilMs

    /**
     * The view's real hide decision: the plain [isIslandViewHidden] verdict with the waking-card count, except
     * that the shrink-back linger keeps the view up a moment longer. This is what the service applies.
     */
    fun effectiveHidden(
        settings: SmartIslandSettings,
        isLocked: Boolean,
        isLandscape: Boolean,
        wakeCount: Int,
        exemptMode: ExemptMode,
        nowMs: Long,
        shrinkLingerUntilMs: Long
    ): Boolean =
        isIslandViewHidden(settings, isLocked, isLandscape, wakeCount, exemptMode) &&
            !viewStaysForShrink(
                isLandscape = isLandscape,
                showInLandscape = settings.showInLandscape,
                showNotificationCardsInLandscape = settings.showNotificationCardsInLandscape,
                wakeCount = wakeCount,
                nowMs = nowMs,
                shrinkLingerUntilMs = shrinkLingerUntilMs
            )
}
