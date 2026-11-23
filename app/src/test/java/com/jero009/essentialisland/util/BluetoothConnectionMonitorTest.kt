/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import com.jero009.essentialisland.data.SmartIslandNotificationRepository
import com.jero009.essentialisland.data.SmartIslandSettings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Multi-device behaviour of the Bluetooth card: the card follows the device the user connected last
 * (when the system told us), falls back to a device that is still connected, and clears only when
 * nothing countable remains.
 */
class BluetoothConnectionMonitorTest {

    private val repo = SmartIslandNotificationRepository()
    private val context = mockk<Context>()
    private val manager = mockk<BluetoothManager>()
    private val settings = SmartIslandSettings.Default.copy(enabled = true)
    private var connected: List<BluetoothDevice> = emptyList()

    @Before
    fun setUp() {
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns
            PackageManager.PERMISSION_GRANTED
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager
        every { manager.getConnectedDevices(any()) } answers { connected }
    }

    private fun audioDevice(address: String, name: String): BluetoothDevice {
        val device = mockk<BluetoothDevice>()
        val btClass = mockk<BluetoothClass>()
        every { btClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { btClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { device.bluetoothClass } returns btClass
        every { device.name } returns name
        every { device.address } returns address
        return device
    }

    private fun card(): com.jero009.essentialisland.model.IslandNotification? =
        repo.notifications.value.find { it.key == "system_bluetooth" }

    @Test
    fun preferFollowsTheDeviceThatJustConnectedInsteadOfTheStackOrder() {
        val earbuds = audioDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        val headphones = audioDevice("BB:BB:BB:BB:BB:02", "Sony WH-1000XM5")
        connected = listOf(earbuds, headphones)
        val monitor = BluetoothConnectionMonitor(context, CoroutineScope(Dispatchers.Unconfined), repo) { settings }

        // Without a hint the card shows the stack's first device.
        monitor.refreshNow()
        assertEquals("Galaxy Buds Pro", card()?.title)

        // The headphones connect: the system tells us their address and the card follows them at once.
        monitor.prefer("BB:BB:BB:BB:BB:02")
        assertEquals("Sony WH-1000XM5", card()?.title)
    }

    @Test
    fun preferredAddressThatDisappearsFallsBackToAConnectedDevice() {
        val earbuds = audioDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(earbuds)
        val monitor = BluetoothConnectionMonitor(context, CoroutineScope(Dispatchers.Unconfined), repo) { settings }

        monitor.prefer("BB:BB:BB:BB:BB:02")
        assertEquals("Galaxy Buds Pro", card()?.title)
    }

    @Test
    fun cardClearsOnlyWhenNoCountableDeviceRemains() {
        val earbuds = audioDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(earbuds)
        val monitor = BluetoothConnectionMonitor(context, CoroutineScope(Dispatchers.Unconfined), repo) { settings }
        monitor.refreshNow()
        assertEquals("Galaxy Buds Pro", card()?.title)

        connected = emptyList()
        monitor.refreshNow()
        assertNull(card())
    }

    @Test
    fun ignoredDevicesNeverOwnTheCard() {
        val watch = mockk<BluetoothDevice>()
        val watchClass = mockk<BluetoothClass>()
        every { watchClass.majorDeviceClass } returns BluetoothClass.Device.Major.WEARABLE
        every { watchClass.deviceClass } returns BluetoothClass.Device.WEARABLE_WRIST_WATCH
        every { watch.bluetoothClass } returns watchClass
        every { watch.name } returns "Galaxy Watch 7"
        every { watch.address } returns "CC:CC:CC:CC:CC:03"
        val earbuds = audioDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(watch, earbuds)
        val monitor = BluetoothConnectionMonitor(context, CoroutineScope(Dispatchers.Unconfined), repo) { settings }

        monitor.refreshNow()
        assertEquals("Galaxy Buds Pro", card()?.title)
    }

    /** Watches and trackers hold a GATT link around the clock — GATT devices must never reach the card. */
    @Test
    fun gattOnlyDevicesNeverOwnTheCard() {
        val watch = mockk<BluetoothDevice>()
        every { watch.name } returns "Galaxy Watch 7"
        every { watch.address } returns "CC:CC:CC:CC:CC:03"
        every { manager.getConnectedDevices(BluetoothProfile.GATT) } returns listOf(watch)
        every { manager.getConnectedDevices(BluetoothProfile.A2DP) } returns emptyList()
        every { manager.getConnectedDevices(BluetoothProfile.HEADSET) } returns emptyList()
        val monitor = BluetoothConnectionMonitor(context, CoroutineScope(Dispatchers.Unconfined), repo) { settings }

        monitor.refreshNow()
        assertNull(card())
    }

    /** Modern TWS headphones connect over LE Audio and never appear on A2DP — the profile must be queried. */
    @Test
    fun audioProfilesIncludeLeAudioOnModernAndroidsOnly() {
        val profiles = BluetoothConnectionMonitor.audioProfiles(37)
        assertTrue(profiles.contains(BluetoothProfile.LE_AUDIO))
        assertTrue(profiles.contains(BluetoothProfile.A2DP))
        assertTrue(profiles.contains(BluetoothProfile.HEADSET))
        assertFalse(profiles.contains(BluetoothProfile.GATT))
        assertEquals(listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET, BluetoothProfile.LE_AUDIO), profiles)
        assertFalse(BluetoothConnectionMonitor.audioProfiles(26).contains(BluetoothProfile.LE_AUDIO))
    }
}
