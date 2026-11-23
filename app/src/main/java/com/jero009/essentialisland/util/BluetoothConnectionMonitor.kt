/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
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
        if (!settings.enabled || !settings.isEventEnabled(IslandEvent.BLUETOOTH.id)) {
            if (hasCard) repository.removeNotification(KEY)
            return
        }
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return
        val candidates = sequenceOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.GATT)
            .mapNotNull { profile ->
                runCatching { manager.getConnectedDevices(profile) }.getOrNull()
            }
            .flatten()
            .distinctBy { it.address }
            .filter { candidate ->
                val candidateName = runCatching { candidate.name }.getOrNull().orEmpty()
                !BtDeviceClassifier.isIgnoredDevice(candidate, candidateName)
            }
            .toList()
        if (candidates.isEmpty()) {
            // Nothing connected (that we count): clear the card if the broadcast left one behind.
            preferredAddress = null
            if (hasCard) repository.removeNotification(KEY)
            return
        }
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

    companion object {
        private const val TAG = "BluetoothConnectionMonitor"
        private const val KEY = "system_bluetooth"
        private const val POLL_MS = 15_000L
    }
}
