/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.model

import com.jero009.essentialisland.data.SmartIslandSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandscapeSurfacingTest {
    private val allOn: (String) -> Boolean = { true }

    private fun card(mode: IslandMode) = IslandNotification(
        key = "k_${mode.name}",
        packageName = "com.example",
        appName = "Example",
        title = "t",
        text = "t",
        mode = mode,
        timeMillis = 0L
    )

    // --- Which cards wake the hidden landscape island ---

    @Test fun appNotificationsWake() {
        assertTrue(LandscapeSurfacing.wakesHiddenIsland(card(IslandMode.Notification), allOn))
    }

    @Test fun incomingCallsWake() {
        assertTrue(LandscapeSurfacing.wakesHiddenIsland(card(IslandMode.IncomingCall), allOn))
    }

    @Test fun deviceStateCardsNeverWake() {
        val deviceState = listOf(
            IslandMode.Battery, IslandMode.Bluetooth, IslandMode.Hotspot, IslandMode.Flashlight,
            IslandMode.Music, IslandMode.Navigation, IslandMode.LiveActivity, IslandMode.DownloadUpload,
            IslandMode.Timer, IslandMode.Stopwatch, IslandMode.ScreenRecording, IslandMode.AudioRecording,
            IslandMode.Empty
        )
        deviceState.forEach { mode ->
            assertFalse("mode $mode must not wake the hidden island", LandscapeSurfacing.wakesHiddenIsland(card(mode), allOn))
        }
    }

    @Test fun wakeCountCountsOnlyWakingCards() {
        val list = listOf(
            card(IslandMode.Notification),
            card(IslandMode.Battery),
            card(IslandMode.IncomingCall),
            card(IslandMode.Bluetooth),
            card(IslandMode.Timer)
        )
        assertEquals(2, LandscapeSurfacing.wakeCount(list, allOn))
    }

    @Test fun wakeCountOfNothingIsZero() {
        assertEquals(0, LandscapeSurfacing.wakeCount(emptyList(), allOn))
    }

    @Test fun perEventLandscapeSwitchIsHonoured() {
        val notificationOff: (String) -> Boolean = { it != IslandEvent.NOTIFICATION.id }
        val callOff: (String) -> Boolean = { it != IslandEvent.CALL.id }
        // The notification event switched off for landscape: the app card no longer wakes the island.
        assertFalse(LandscapeSurfacing.wakesHiddenIsland(card(IslandMode.Notification), notificationOff))
        // The call event switched off for landscape: the call no longer wakes it either.
        assertFalse(LandscapeSurfacing.wakesHiddenIsland(card(IslandMode.IncomingCall), callOff))
        // Other events being off changes nothing for a waking card.
        assertTrue(LandscapeSurfacing.wakesHiddenIsland(card(IslandMode.Notification), callOff))
        assertTrue(LandscapeSurfacing.wakesHiddenIsland(card(IslandMode.IncomingCall), notificationOff))
    }

    // --- The shrink-back linger ---

    @Test fun viewStaysWhileTheLingerRuns() {
        assertTrue(
            LandscapeSurfacing.viewStaysForShrink(
                isLandscape = true, showInLandscape = false, showNotificationCardsInLandscape = true,
                wakeCount = 0, nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
            )
        )
    }

    @Test fun portraitNeverLingers() {
        assertFalse(
            LandscapeSurfacing.viewStaysForShrink(
                isLandscape = false, showInLandscape = false, showNotificationCardsInLandscape = true,
                wakeCount = 0, nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
            )
        )
    }

    @Test fun visibleLandscapePillNeverLingers() {
        // With the pill itself shown in landscape, its own idle-circle hand-over applies, not the linger.
        assertFalse(
            LandscapeSurfacing.viewStaysForShrink(
                isLandscape = true, showInLandscape = true, showNotificationCardsInLandscape = true,
                wakeCount = 0, nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
            )
        )
    }

    @Test fun cardsToggleOffNeverLingers() {
        assertFalse(
            LandscapeSurfacing.viewStaysForShrink(
                isLandscape = true, showInLandscape = false, showNotificationCardsInLandscape = false,
                wakeCount = 0, nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
            )
        )
    }

    @Test fun wakingCardsBeatTheLinger() {
        assertFalse(
            LandscapeSurfacing.viewStaysForShrink(
                isLandscape = true, showInLandscape = false, showNotificationCardsInLandscape = true,
                wakeCount = 2, nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
            )
        )
    }

    @Test fun expiredLingerHides() {
        assertFalse(
            LandscapeSurfacing.viewStaysForShrink(
                isLandscape = true, showInLandscape = false, showNotificationCardsInLandscape = true,
                wakeCount = 0, nowMs = 1_500L, shrinkLingerUntilMs = 1_500L
            )
        )
    }

    // --- The combined decision the service applies ---

    private val hiddenIsland = SmartIslandSettings(showInLandscape = false)

    @Test fun effectiveHiddenKeepsTheViewUpDuringTheLinger() {
        val hidden = LandscapeSurfacing.effectiveHidden(
            settings = hiddenIsland,
            isLocked = false,
            isLandscape = true,
            wakeCount = 0,
            exemptMode = ExemptMode.None,
            nowMs = 1_000L,
            shrinkLingerUntilMs = 1_500L
        )
        assertFalse(hidden)
    }

    @Test fun effectiveHiddenHidesAfterTheLinger() {
        val hidden = LandscapeSurfacing.effectiveHidden(
            settings = hiddenIsland,
            isLocked = false,
            isLandscape = true,
            wakeCount = 0,
            exemptMode = ExemptMode.None,
            nowMs = 1_600L,
            shrinkLingerUntilMs = 1_500L
        )
        assertTrue(hidden)
    }

    @Test fun effectiveHiddenLandscapeWakesOnlyForAppCards() {
        fun hiddenWith(vararg modes: IslandMode): Boolean {
            val wake = LandscapeSurfacing.wakeCount(modes.map { card(it) }, allOn)
            return LandscapeSurfacing.effectiveHidden(
                settings = hiddenIsland,
                isLocked = false,
                isLandscape = true,
                wakeCount = wake,
                exemptMode = ExemptMode.None,
                nowMs = 1_000L,
                shrinkLingerUntilMs = 0L
            )
        }
        // A system-state card alone keeps the island hidden; an app notification surfaces it.
        assertTrue(hiddenWith(IslandMode.Battery, IslandMode.Bluetooth))
        assertFalse(hiddenWith(IslandMode.Battery, IslandMode.Notification))
    }

    @Test fun effectiveHiddenPortraitIgnoresTheLingerAndWakeCount() {
        // Portrait was never hidden by the card-surfacing branch; nothing about the linger may change that.
        assertFalse(
            LandscapeSurfacing.effectiveHidden(
                settings = SmartIslandSettings(),
                isLocked = false,
                isLandscape = false,
                wakeCount = 0,
                exemptMode = ExemptMode.None,
                nowMs = 1_000L,
                shrinkLingerUntilMs = 1_500L
            )
        )
    }

    @Test fun effectiveHiddenLockScreenAndExemptStillHide() {
        val locked = LandscapeSurfacing.effectiveHidden(
            settings = SmartIslandSettings(showOnLockScreen = false),
            isLocked = true, isLandscape = true, wakeCount = 1, exemptMode = ExemptMode.None,
            nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
        )
        val exempt = LandscapeSurfacing.effectiveHidden(
            settings = SmartIslandSettings(),
            isLocked = false, isLandscape = true, wakeCount = 1, exemptMode = ExemptMode.Off,
            nowMs = 1_000L, shrinkLingerUntilMs = 1_500L
        )
        assertTrue(locked)
        assertTrue(exempt)
    }
}
