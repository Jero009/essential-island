/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice

/**
 * Shared Bluetooth device classification for the ACL broadcast receiver and the polling monitor:
 * label ("Earbuds" / "Headphones" / …), icon category (bt_earbuds / bt_headphones / bluetooth),
 * the wearable/HID ignore filter, and the hidden battery read.
 */
object BtDeviceClassifier {

    private val earbudNameHints = listOf(
        "buds", "pod", "airpods", "freebuds", "earbuds", "ear stick", "earstick",
        "nothing ear", "ear (a)", "ear (2)", "tws"
    )

    /** Class said "headphones", but over-ear cans and TWS earbuds share that class — the name decides. */
    @SuppressLint("MissingPermission")
    fun category(device: BluetoothDevice?, deviceName: String): String {
        val audioHeadphones = runCatching { device?.bluetoothClass?.deviceClass }
            .getOrNull() == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        val lower = deviceName.lowercase()
        val earbudByName = earbudNameHints.any { lower.contains(it) }
        return when {
            audioHeadphones && earbudByName -> "bt_earbuds"
            audioHeadphones -> "bt_headphones"
            earbudByName -> "bt_earbuds"
            else -> "bluetooth"
        }
    }

    fun kindLabel(category: String): String = when (category) {
        "bt_earbuds" -> "Earbuds"
        "bt_headphones" -> "Headphones"
        else -> "Bluetooth"
    }

    fun labelFor(device: BluetoothDevice?, deviceName: String): String =
        kindLabel(category(device, deviceName))

    fun connectedText(category: String, batteryLevel: Int): String = buildString {
        append(kindLabel(category))
        append(" connected")
        if (batteryLevel in 0..100) append(" • $batteryLevel%")
    }

    /** Smartwatches, bands and HID peripherals must not hijack the island with connection cards. */
    @SuppressLint("MissingPermission")
    fun isIgnoredDevice(device: BluetoothDevice?, deviceName: String): Boolean {
        if (device == null) return false
        val bluetoothClass = runCatching { device.bluetoothClass }.getOrNull()
        val majorClass = bluetoothClass?.majorDeviceClass
        val deviceClass = bluetoothClass?.deviceClass

        if (majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO) return false
        if (majorClass == BluetoothClass.Device.Major.WEARABLE ||
            majorClass == BluetoothClass.Device.Major.HEALTH ||
            majorClass == BluetoothClass.Device.Major.PERIPHERAL
        ) return true

        if (deviceClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH ||
            deviceClass == BluetoothClass.Device.WEARABLE_PAGER ||
            deviceClass == BluetoothClass.Device.WEARABLE_JACKET ||
            deviceClass == BluetoothClass.Device.WEARABLE_HELMET ||
            deviceClass == BluetoothClass.Device.WEARABLE_GLASSES
        ) return true

        val lower = deviceName.lowercase()
        val wearableKeywords = listOf(
            "watch", "smartwatch", "wear os", "fitbit", "garmin", "amazfit",
            "whoop", "oura", "smart band", "smartband", "mi band", "miband",
            "honor band", "huawei band", "galaxy fit", "band"
        )
        if (wearableKeywords.any { lower.contains(it) }) return true

        val peripheralKeywords = listOf("mouse", "keyboard", "trackpad", "gamepad", "controller", "stylus", "s pen")
        if (peripheralKeywords.any { lower.contains(it) }) return true

        return false
    }

    /** Hidden API read; returns -1 when the device does not report a battery. */
    @SuppressLint("MissingPermission")
    fun batteryLevel(device: BluetoothDevice?): Int {
        val fromExtra = runCatching { device?.javaClass }.getOrNull()
        if (fromExtra == null) return -1
        return runCatching {
            val method = device?.javaClass?.getMethod("getBatteryLevel")
            (method?.invoke(device) as? Int) ?: -1
        }.getOrDefault(-1)
    }
}
