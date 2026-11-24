/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The earbuds state store backs the expanded Bluetooth card: v1.12.0 froze the expand morph when the
 * ANC/EQ/in-ear read landed a couple of seconds after the card was shown (the arriving rows grew the
 * measured height mid-session). The rules pinned here: the store keeps answered reads per device, a
 * failed read never wipes known state, and a read landing mid-session on a cold store must NOT change
 * the session's layout — that is the expand-with-hidden-ANC-panel regression guard.
 */
class EarbudsStateStoreTest {

    @Before
    fun setUp() {
        EarbudsStateStore.resetForTest()
    }

    private fun put(name: String, ancMode: Int?, eqPreset: Int?, inEarOn: Boolean?) {
        EarbudsStateStore.put(name, EarbudsStateStore.Snapshot(ancMode, eqPreset, inEarOn))
    }

    @Test
    fun storeRoundTripsAndIsolatesDevices() {
        put("Nothing Ear (2)", ancMode = 4, eqPreset = 0, inEarOn = true)
        put("CMF Buds 2", ancMode = 1, eqPreset = 3, inEarOn = false)

        assertEquals(EarbudsStateStore.Snapshot(4, 0, true), EarbudsStateStore.snapshot("Nothing Ear (2)"))
        assertEquals(EarbudsStateStore.Snapshot(1, 3, false), EarbudsStateStore.snapshot("CMF Buds 2"))
        assertNull(EarbudsStateStore.snapshot("Nothing Ear (a)"))
        assertTrue(EarbudsStateStore.isKnown("Nothing Ear (2)"))
        assertFalse(EarbudsStateStore.isKnown("Nothing Ear (a)"))
    }

    @Test
    fun failedRefreshKeepsTheKnownStateAndMarksTheAttempt() {
        put("Nothing Headphone (a)", ancMode = 1, eqPreset = 2, inEarOn = true)

        val result = runBlocking(Dispatchers.Unconfined) {
            EarbudsStateStore.refresh("Nothing Headphone (a)") { null }
        }

        // Nothing answered: the call hands back the kept state instead of wiping it.
        assertEquals(EarbudsStateStore.Snapshot(1, 2, true), result)
        assertEquals(
            EarbudsStateStore.Snapshot(1, 2, true),
            EarbudsStateStore.snapshot("Nothing Headphone (a)")
        ) // a later failed read must not wipe the known state
        assertTrue(EarbudsStateStore.hasBeenAttempted("Nothing Headphone (a)"))
    }

    @Test
    fun refreshStoresOnlyAnsweredReads() {
        val result = runBlocking(Dispatchers.Unconfined) {
            EarbudsStateStore.refresh("Nothing Ear") {
                EarbudsStateStore.Snapshot(ancMode = 7, eqPreset = 5, inEarOn = false)
            }
        }
        assertEquals(EarbudsStateStore.Snapshot(7, 5, false), result)
        assertEquals(EarbudsStateStore.Snapshot(7, 5, false), EarbudsStateStore.snapshot("Nothing Ear"))
    }

    /**
     * THE REGRESSION GUARD: a read landing mid-session on a store that was cold at composition must
     * leave the session layout untouched (the rows would grow the card under the expanded shell and
     * freeze the expand morph). With a warm store the fresh read updates the values in place.
     */
    @Test
    fun sessionLayoutIgnoresMidSessionReadsWhenTheStoreWasCold() {
        val headerOnly = EarbudsStateStore.Snapshot(null, null, null)
        val fullPanel = EarbudsStateStore.Snapshot(ancMode = 4, eqPreset = 0, inEarOn = true)

        // Cold at compose (no answer yet): the late read must not add rows to the live session.
        val cold = EarbudsStateStore.sessionLayoutAfterRead(coldAtCompose = true, before = headerOnly, read = fullPanel)
        assertEquals(headerOnly, cold)

        // Warm at compose (pre-warmed store): the fresh read updates the live values.
        val warm = EarbudsStateStore.sessionLayoutAfterRead(coldAtCompose = false, before = headerOnly, read = fullPanel)
        assertEquals(fullPanel, warm)

        // A failed mid-session read on a warm store keeps the current rows.
        val warmFailed = EarbudsStateStore.sessionLayoutAfterRead(coldAtCompose = false, before = fullPanel, read = null)
        assertEquals(fullPanel, warmFailed)
    }
}