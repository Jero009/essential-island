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
import android.media.AudioDeviceInfo
import android.media.AudioManager
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
 * Owns the "system_bluetooth" card. Candidates come from AudioManager's registered audio devices — the same
 * source that drives [android.media.AudioDeviceCallback] — because that is the one path proven to work on
 * Nothing OS: ACL broadcasts are unreliable there, and BluetoothManager's per-profile connected-device
 * queries came back empty on a FroggerPro / Android 17 even with the earbuds connected and BLUETOOTH_CONNECT
 * granted (v1.9.1–v1.11.4 never produced a candidate there; the failure was silent by design). A watch that
 * only holds a GATT link never registers an audio route, so the watch exclusion holds by construction; the
 * name/class rules in [BtDeviceClassifier.isIgnoredDevice] still catch wearables that DO register a route.
 * Every tick decision is logged (via [AppLogRecorder.recordCritical], always on) and deduped by state.
 */
class BluetoothConnectionMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repository: INotificationRepository,
    private val settingsProvider: () -> SmartIslandSettings,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) {
    private var job: Job? = null

    /** The device the card should follow: the one the user connected most recently, when known. */
    @Volatile
    private var preferredAddress: String? = null

    @Volatile private var settingsUnloadedLogged = false
    @Volatile private var disabledLogged = false
    @Volatile private var permissionLogged = false
    @Volatile private var audioMissingLogged = false
    @Volatile private var addressUnsupportedLogged = false
    @Volatile private var lastIdleSignature: String? = null
    @Volatile private var lastFilteredSignature: String? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                runCatchingLogged(TAG, "bluetooth poll failed") { tick("poll") }
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
        runCatchingLogged(TAG, "bluetooth prefer-refresh failed") { tick("prefer") }
    }

    /** Reconciles the card immediately (a device may have disconnected); the poll stays as the safety net. */
    fun refreshNow() {
        runCatchingLogged(TAG, "bluetooth refresh failed") { tick("refresh") }
    }

    private fun tick(reason: String) {
        val settings = settingsProvider()
        if (settings == null || settings == SmartIslandSettings.Default) {
            // DataStore has not delivered the real settings yet (async load): deciding anything now
            // would log false verdicts against the unloaded default (island "off"). Wait for the poll.
            if (!settingsUnloadedLogged) {
                settingsUnloadedLogged = true
                AppLogRecorder.recordCritical("I", TAG, "tick($reason): settings not loaded yet — the next tick decides")
            }
            return
        }
        val hasCard = repository.notifications.value.any { it.key == KEY }
        if (!settings.enabled || !settings.isEventEnabled(IslandEvent.BLUETOOTH.id)) {
            if (!disabledLogged) {
                disabledLogged = true
                AppLogRecorder.recordCritical(
                    "I",
                    TAG,
                    "tick($reason): Bluetooth card off (island=${settings.enabled}, " +
                        "btEvent=${settings.isEventEnabled(IslandEvent.BLUETOOTH.id)})"
                )
            }
            if (hasCard) repository.removeNotification(KEY)
            return
        }
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (!permissionLogged) {
                permissionLogged = true
                AppLogRecorder.recordCritical("W", TAG, "tick($reason): Nearby devices (BLUETOOTH_CONNECT) is not granted")
            }
            return
        }
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: run {
            if (!audioMissingLogged) {
                audioMissingLogged = true
                AppLogRecorder.recordCritical("W", TAG, "tick($reason): AudioManager unavailable — the card cannot enumerate devices")
            }
            return
        }

        val btAudioDevices = connectedBluetoothAudioDevices(audio)
        if (btAudioDevices.isEmpty()) {
            lastFilteredSignature = null
            val enumerationFailed = lastIdleSignature?.startsWith("error:") == true
            if (!enumerationFailed && lastIdleSignature != "empty") {
                lastIdleSignature = "empty"
                val total = runCatching {
                    audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS or AudioManager.GET_DEVICES_INPUTS).size
                }.getOrDefault(-1)
                AppLogRecorder.recordCritical(
                    "I",
                    TAG,
                    "tick($reason): no Bluetooth audio device is registered (audio devices total: $total)"
                )
            }
            if (hasCard) {
                AppLogRecorder.recordCritical("I", TAG, "tick($reason): Bluetooth card cleared — no Bluetooth audio device is connected")
                repository.removeNotification(KEY)
            }
            return
        }
        lastIdleSignature = null

        if (sdkInt < Build.VERSION_CODES.P) {
            // AudioDeviceInfo.getAddress() is API 28+; without it the device cannot be matched or named.
            if (!addressUnsupportedLogged) {
                addressUnsupportedLogged = true
                AppLogRecorder.recordCritical(
                    "W",
                    TAG,
                    "tick($reason): this Android version cannot resolve Bluetooth audio addresses (needs API 28)"
                )
            }
            return
        }

        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val candidates = btAudioDevices.mapNotNull { info ->
            val address = info.safeAddress() ?: return@mapNotNull null
            val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return@mapNotNull null
            val name = runCatching { device.name }.getOrNull().orEmpty().ifBlank { "Bluetooth Device" }
            Triple(address, name, device)
        }
        if (candidates.isEmpty()) {
            // Routes exist but no address/name could be resolved — impossible to build a card.
            val signature = "unresolved"
            if (lastFilteredSignature != signature) {
                lastFilteredSignature = signature
                AppLogRecorder.recordCritical(
                    "W",
                    TAG,
                    "tick($reason): ${btAudioDevices.size} Bluetooth audio route(s) registered but none resolved to a device " +
                        "(adapter ${if (adapter == null) "unavailable" else "present"})"
                )
            }
            if (hasCard) repository.removeNotification(KEY)
            return
        }

        val kept = candidates.filter { (_, name, device) -> !BtDeviceClassifier.isIgnoredDevice(device, name) }
        if (kept.isEmpty()) {
            // Devices ARE connected but none survived the ignore rules — the state that used to eat the
            // card in silence. Logged when the seen list changes, not on every poll.
            val seen = candidates.map { (_, name, _) -> name }
            val signature = "$seen"
            if (signature != lastFilteredSignature) {
                lastFilteredSignature = signature
                AppLogRecorder.recordCritical(
                    "W",
                    TAG,
                    "tick($reason): Bluetooth devices are connected but none survived the filter: seen: $seen"
                )
            }
            preferredAddress = null
            if (hasCard) {
                AppLogRecorder.recordCritical("I", TAG, "tick($reason): Bluetooth card cleared — all connected devices are filtered out")
                repository.removeNotification(KEY)
            }
            return
        }
        lastFilteredSignature = null

        // The most recently connected device wins; an address that has dropped off (or an unknown one,
        // e.g. right after a process restart) falls back to whatever is still connected.
        val preferred = preferredAddress?.let { address -> kept.firstOrNull { (candidate, _, _) -> candidate == address } }
        if (preferred == null) preferredAddress = null
        val (address, name, device) = preferred ?: kept.first()

        val category = BtDeviceClassifier.category(device, name)
        val battery = BtDeviceClassifier.batteryLevel(device)
        val text = BtDeviceClassifier.connectedText(category, battery)

        val existing = repository.notifications.value.find { it.key == KEY }
        if (existing != null && existing.title == name && existing.text == text && existing.category == category) return
        AppLogRecorder.recordCritical(
            "I",
            TAG,
            "tick($reason): Bluetooth card ${if (existing == null) "shown" else "updated"}: '$name' " +
                "($category${if (battery in 0..100) ", $battery%" else ""}) of " +
                "candidates=${kept.map { (candidate, candidateName, _) -> "$candidateName@$candidate" }}, " +
                "preferred=${preferredAddress ?: "none"}"
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
        // Pre-warm the earbuds state (ANC mode, EQ preset, in-ear) so the expanded card composes at its
        // final height from the first frame — a read landing after the card is shown grows the measured
        // height under the expanded shell and freezes the expand morph. Once per device per process;
        // the expanded card re-reads on open.
        if (NothingAnc.isSupportedDevice(name) && !EarbudsStateStore.hasBeenAttempted(name)) {
            scope.launch { EarbudsStateStore.refreshFromDevice(context, name) }
        }
    }

    /** Every audio route backed by a Bluetooth device (A2DP/SCO now, LE Audio from API 31), deduped by address. */
    private fun connectedBluetoothAudioDevices(audio: AudioManager): List<AudioDeviceInfo> =
        runCatching {
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS or AudioManager.GET_DEVICES_INPUTS)
                .filter { it.type in bluetoothAudioTypes() }
                .distinctBy { it.dedupeKey() }
        }.getOrElse { error ->
            // Decisive logging: the old per-profile queries failed exactly here, in silence.
            val signature = "error:${error.javaClass.simpleName}"
            if (lastIdleSignature != signature) {
                lastIdleSignature = signature
                AppLogRecorder.recordCritical(
                    "W",
                    TAG,
                    "tick: enumerating audio devices failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}"
                )
            }
            emptyList()
        }

    /** The MAC address of the audio route, or null below API 28 / when the route has none. */
    @SuppressLint("NewApi") // AudioDeviceInfo.getAddress is API 28; [sdkInt] is checked here.
    private fun AudioDeviceInfo.safeAddress(): String? =
        if (sdkInt >= Build.VERSION_CODES.P) address.orEmpty().ifBlank { null } else null

    /** Stable identity of a route for dedupe: the address when available, else its type. */
    @SuppressLint("NewApi")
    private fun AudioDeviceInfo.dedupeKey(): String = safeAddress() ?: "type:$type"

    @SuppressLint("InlinedApi")
    private fun bluetoothAudioTypes(): Set<Int> = buildSet {
        add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
        add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        if (sdkInt >= Build.VERSION_CODES.S) {
            add(AudioDeviceInfo.TYPE_BLE_HEADSET)
            add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
    }

    internal companion object {
        private const val TAG = "BluetoothConnectionMonitor"
        private const val KEY = "system_bluetooth"
        private const val POLL_MS = 15_000L

        /**
         * The audio profiles the disconnect-handover in [com.jero009.essentialisland.service.SystemEventReceiver]
         * still queries. The connection card itself enumerates via AudioManager instead (see the class comment).
         */
        @SuppressLint("InlinedApi")
        internal fun audioProfiles(sdkInt: Int): List<Int> = buildList {
            add(BluetoothProfile.A2DP)
            add(BluetoothProfile.HEADSET)
            if (sdkInt >= Build.VERSION_CODES.S) add(BluetoothProfile.LE_AUDIO)
        }
    }
}