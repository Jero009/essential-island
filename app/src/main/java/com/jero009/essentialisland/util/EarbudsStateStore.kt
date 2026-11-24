/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Remembers the last-read earbuds state (ANC mode, EQ preset, in-ear switch) per device so the expanded
 * card composes at its final height from the first frame. v1.12.0 froze the expand morph when the
 * RFCOMM read landed a couple of seconds AFTER the card was shown: the arriving rows grew the measured
 * height mid-shown and the shell never picked the new target up. The state is therefore read when the
 * connection card appears (see [BluetoothConnectionMonitor]) and the expanded card only ever renders
 * rows from data that was already known when it composed; a read that lands mid-session updates the
 * store for the NEXT expansion, never the current layout.
 */
object EarbudsStateStore {

    data class Snapshot(val ancMode: Int?, val eqPreset: Int?, val inEarOn: Boolean?)

    private val snapshots = MutableStateFlow<Map<String, Snapshot>>(emptyMap())
    val states: StateFlow<Map<String, Snapshot>> = snapshots

    @Volatile
    private var inFlight: String? = null

    private val attempted = java.util.Collections.synchronizedSet(HashSet<String>())

    /** True once a read was attempted for [name] this process (answered or not) — pre-warm fires once. */
    fun hasBeenAttempted(name: String): Boolean = attempted.contains(name)

    private fun markAttempted(name: String) {
        attempted.add(name)
    }

    /** The stored state for [name], or null when this device has never answered a read. */
    fun snapshot(name: String): Snapshot? = snapshots.value[name]

    fun isKnown(name: String): Boolean = snapshot(name) != null

    fun put(name: String, snapshot: Snapshot?) {
        if (snapshot == null) return
        snapshots.value = snapshots.value + (name to snapshot)
    }

    /**
     * Runs [reader] (the blocking RFCOMM read) and stores the result. A read that does not answer
     * leaves the store untouched: a device that answered once keeps its state instead of losing it
     * to a later failed read.
     */
    suspend fun refresh(name: String, reader: suspend () -> Snapshot?): Snapshot? {
        markAttempted(name)
        if (inFlight == name) return snapshot(name)
        inFlight = name
        try {
            val read = reader() ?: return snapshot(name)
            put(name, read)
            return read
        } finally {
            inFlight = null
        }
    }

    /** The one-shot read of all three values over the vendor RFCOMM channel; null when nothing answered. */
    suspend fun readFromDevice(context: Context, name: String): Snapshot? = withContext(Dispatchers.IO) {
        val device = NothingAnc.findConnectedDevice(context, name) ?: return@withContext null
        val client = NothingAncClient(device)
        try {
            if (!client.connect()) return@withContext null
            val cmf = NothingAnc.isCmfDevice(name)
            Snapshot(
                ancMode = client.readAnc()?.let { NothingAnc.modeFromPayload(it, 0x401E) },
                eqPreset = client.readEqPreset(cmf)?.let { NothingAnc.presetFromPayload(it) },
                inEarOn = client.readInEar()?.let { NothingAnc.inEarFromPayload(it) }
            )
        } finally {
            client.close()
        }
    }

    /** Convenience used by callers that read straight from the device. */
    suspend fun refreshFromDevice(context: Context, name: String): Snapshot? =
        refresh(name) { readFromDevice(context, name) }

    /**
     * The session's rows after a read landed. When the store was COLD at composition the session keeps
     * its current layout — rows that appear mid-session grow the measured height under an expanded shell
     * and freeze the expand morph (the v1.12.0 regression); the read's data is already in the store and
     * shows on the next expansion. When the store was warm the rows were part of the first measure, so
     * the fresh read updates them in place (values change, not the row set — the height class holds).
     */
    fun sessionLayoutAfterRead(coldAtCompose: Boolean, before: Snapshot?, read: Snapshot?): Snapshot? =
        if (coldAtCompose) before else (read ?: before)

    /** Test isolation only: clears every stored device state and attempt marker. */
    internal fun resetForTest() {
        snapshots.value = emptyMap()
        attempted.clear()
        inFlight = null
    }
}
