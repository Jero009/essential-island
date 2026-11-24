/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.jero009.essentialisland.data.INotificationRepository
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.model.IslandEvent
import com.jero009.essentialisland.model.IslandMode
import com.jero009.essentialisland.model.IslandNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls the actually-connected Bluetooth devices instead of relying on ACTION_ACL broadcasts, which
 * several OEM skins (Nothing OS included) do not reliably deliver to third-party apps on Android 12+.
 * Keeps the "system_bluetooth" card in sync with reality every [POLL_MS]; harmless when the broadcast
 * receiver already handled it (same key, same content = no churn).
 */
class BluetoothConnectionMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settingsProvider: () -> SmartIslandSettings
) {
    private var job: Job? = null

    /** The device the card should follow: the one the user connected most recently, when known. */
    @Volatile
    private var preferredAddress: String? = null

    /** Warned once per process until the permission turns up, so the log does not fill with repeats. */
    @Volatile
    private var permissionLogged = false

    /** Warned once per process when the island or the Bluetooth event switch is off. */
    @Volatile
    private var disabledLogged = false

    /** Warned once per process when the BluetoothManager service is unavailable. */
    @Volatile
    private var managerLogged = false

    /** The last "connected but everything filtered" signature; the line only repeats when this changes. */
    @Volatile
    private var lastFilteredSignature: String? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                runCatchingLogged(TAG, "bluetooth poll failed") { tick() }
                delay(POLL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    /** Follows [address] from now on and reconciles the card immediately instead of waiting for the next poll. */
    fun prefer(address: String?) {
        preferredAddress = address
        runCatchingLogged(TAG, "bluetooth prefer-refresh failed") { tick() }
    }

    /** Reconciles the card immediately (a device may have disconnected); the poll stays as the safety net. */
    fun refreshNow() {
        runCatchingLogged(TAG, "bluetooth refresh failed") { tick() }
    }

    private fun tick() {
        val settings = settingsProvider()
        val hasCard = repository.notifications.value.any { it.key == KEY }
        if (settings == SmartIslandSettings.Default) {
            // DataStore has not delivered the real settings yet (async load): deciding anything now
            // would log false verdicts against the unloaded default (island "off"). Wait for the poll.
            return
        }
        if (!settings.enabled || !settings.isEventEnabled(IslandEvent.BLUETOOTH.id)) {
            if (!disabledLogged) {
                disabledLogged = true
                AppLogRecorder.recordCritical(
                    "I",
                    TAG,
                    "Bluetooth card off: island enabled=${settings.enabled}, " +
                        "bluetooth event enabled=${settings.isEventEnabled(IslandEvent.BLUETOOTH.id)}"
                )
            }
            if (hasCard) repository.removeNotification(KEY)
            return
        }
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (!permissionLogged) {
                permissionLogged = true
                AppLogRecorder.recordCritical("W", TAG, "Bluetooth card cannot work: Nearby devices (BLUETOOTH_CONNECT) is not granted")
            }
            return
        }

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: run {
            if (!managerLogged) {
                managerLogged = true
                AppLogRecorder.recordCritical("W", TAG, "BluetoothManager service unavailable — the card cannot poll")
            }
            return
        }
        // Audio profiles only. GATT is deliberately excluded: watches, bands and trackers hold a GATT link
        // around the clock and would otherwise permanently own (or block) the connection card.
        // LE Audio (API 31+) is included: modern TWS headphones connect through it and never appear on A2DP.
        val profiles = audioProfiles(Build.VERSION.SDK_INT)
        val perProfileDevices = profiles.map { profile ->
            profile to runCatching { manager.getConnectedDevices(profile) }.getOrNull()
        }
        val perProfile = perProfileDevices.map { (profile, devices) -> profile to (devices?.size ?: -1) }
        val candidates = perProfileDevices
            .mapNotNull { (_, devices) -> devices }
            .flatten()
            .distinctBy { it.address }
            .filter { candidate ->
                val candidateName = runCatching { candidate.name }.getOrNull().orEmpty()
                !BtDeviceClassifier.isIgnoredDevice(candidate, candidateName)
            }
            .toList()
        if (candidates.isEmpty()) {
            preferredAddress = null
            if (perProfile.any { (_, count) -> count > 0 }) {
                // Devices ARE connected but none survived the ignore filter (or the query threw):
                // the exact state that used to eat the card in silence. Log it when it changes.
                val seen = perProfileDevices
                    .mapNotNull { (_, devices) -> devices }
                    .flatten()
                    .distinctBy { it.address }
                    .map { device -> runCatching { device.name }.getOrNull().orEmpty().ifBlank { "?" } }
                val signature = "$perProfile$seen"
                if (signature != lastFilteredSignature) {
                    lastFilteredSignature = signature
                    AppLogRecorder.recordCritical(
                        "W",
                        TAG,
                        "Bluetooth devices are connected but none survived the filter: per profile: $perProfile, seen: $seen"
                    )
                }
            } else {
                lastFilteredSignature = null
            }
            if (hasCard) {
                // Nothing connected (that we count): clear the card if the broadcast left one behind.
                AppLogRecorder.recordCritical("I", TAG, "Bluetooth card cleared: no countable device (per profile: $perProfile)")
                repository.removeNotification(KEY)
            }
            return
        }
        lastFilteredSignature = null
        // The most recently connected device wins; an address that has dropped off (or an unknown one,
        // e.g. right after a process restart) falls back to whatever is still connected.
        val preferred = preferredAddress?.let { address -> candidates.firstOrNull { it.address == address } }
        if (preferred == null) preferredAddress = null
        val device = preferred ?: candidates.first()

        val name = runCatching { device.name }.getOrNull().takeIf { !it.isNullOrBlank() } ?: "Bluetooth Device"
        val category = BtDeviceClassifier.category(device, name)
        val battery = BtDeviceClassifier.batteryLevel(device)
        val text = BtDeviceClassifier.connectedText(category, battery)

        val existing = repository.notifications.value.find { it.key == KEY }
        if (existing != null && existing.title == name && existing.text == text && existing.category == category) return
        AppLogRecorder.recordCritical(
            "I",
            TAG,
            "Bluetooth card: chose '$name' ($category${if (battery in 0..100) ", $battery%" else ""}) " +
                "of candidates=${candidates.map { candidate -> runCatching { candidate.name }.getOrNull().orEmpty().ifBlank { "?" } }} " +
                "per profile: $perProfile, preferred=${preferredAddress ?: "none"}"
        )
        repository.postNotification(
            IslandNotification(
                key = KEY,
                packageName = "com.android.bluetooth",
                appName = "Bluetooth",
                title = name,
                text = text,
                category = category,
                mode = IslandMode.Bluetooth,
                progress = if (battery in 0..100) battery else 0,
                progressMax = 100,
                timeMillis = existing?.timeMillis ?: System.currentTimeMillis()
            ),
            autoExpand = false
        )
    }

    internal companion object {
        private const val TAG = "BluetoothConnectionMonitor"
        private const val KEY = "system_bluetooth"
        private const val POLL_MS = 15_000L

        /**
         * The audio profiles a connection card candidate can come from: classic A2DP/headset, plus LE Audio
         * from API 31 (modern TWS headphones connect through it and never appear on A2DP). GATT is excluded
         * on purpose — watches and trackers hold a GATT link around the clock.
         */
        @SuppressLint("InlinedApi")
        internal fun audioProfiles(sdkInt: Int): List<Int> = buildList {
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            if (sdkInt >= Build.VERSION_CODES.S) add(BluetoothProfile.LE_AUDIO)
        }
    }
}
