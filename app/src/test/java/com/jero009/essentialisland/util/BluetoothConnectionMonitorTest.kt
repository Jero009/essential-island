/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
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
 * Multi-device behaviour of the Bluetooth card: candidates come from AudioManager's registered Bluetooth
 * audio routes (the path proven to work on Nothing OS), the card follows the device the user connected
 * last (when the system told us), falls back to a device that is still connected, clears only when no
 * countable device remains — and every tick decision lands in the diagnostic log.
 */
class BluetoothConnectionMonitorTest {

    private val repo = SmartIslandNotificationRepository()
    private val context = mockk<Context>()
    private val audioManager = mockk<AudioManager>()
    private val manager = mockk<BluetoothManager>()
    private val adapter = mockk<BluetoothAdapter>()
    private val settings = SmartIslandSettings.Default.copy(enabled = true)
    private var connected: List<AudioDeviceInfo> = emptyList()

    /** The API level the tests run the monitor at: P+ so addresses resolve, S+ so LE routes count. */
    private val monitorSdkInt = 34

    private fun monitor(provider: () -> SmartIslandSettings = { settings }) =
        BluetoothConnectionMonitor(context, CoroutineScope(Dispatchers.Unconfined), repo, provider, monitorSdkInt)

    @Before
    fun setUp() {
        AppLogRecorder.resetForTest()
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns
            PackageManager.PERMISSION_GRANTED
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager
        every { manager.adapter } returns adapter
        every { audioManager.getDevices(any()) } answers { connected.toTypedArray() }
    }

    private fun audioRoute(address: String, type: Int = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP): AudioDeviceInfo {
        val info = mockk<AudioDeviceInfo>()
        every { info.type } returns type
        every { info.address } returns address
        return info
    }

    private fun remoteDevice(address: String, name: String): BluetoothDevice {
        val device = mockk<BluetoothDevice>()
        val btClass = mockk<BluetoothClass>()
        every { btClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { btClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { device.bluetoothClass } returns btClass
        every { device.name } returns name
        every { device.address } returns address
        every { adapter.getRemoteDevice(address) } returns device
        return device
    }

    private fun wearableRemoteDevice(address: String, name: String): BluetoothDevice {
        val device = mockk<BluetoothDevice>()
        val btClass = mockk<BluetoothClass>()
        every { btClass.majorDeviceClass } returns BluetoothClass.Device.Major.WEARABLE
        every { btClass.deviceClass } returns BluetoothClass.Device.WEARABLE_WRIST_WATCH
        every { device.bluetoothClass } returns btClass
        every { device.name } returns name
        every { device.address } returns address
        every { adapter.getRemoteDevice(address) } returns device
        return device
    }

    private fun card(): com.jero009.essentialisland.model.IslandNotification? =
        repo.notifications.value.find { it.key == "system_bluetooth" }

    @Test
    fun preferFollowsTheDeviceThatJustConnectedInsteadOfTheStackOrder() {
        remoteDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        remoteDevice("BB:BB:BB:BB:BB:02", "Sony WH-1000XM5")
        connected = listOf(audioRoute("AA:AA:AA:AA:AA:01"), audioRoute("BB:BB:BB:BB:BB:02"))
        val monitor = monitor()

        // Without a hint the card shows the first route.
        monitor.refreshNow()
        assertEquals("Galaxy Buds Pro", card()?.title)

        // The headphones connect: the system tells us their address and the card follows them at once.
        monitor.prefer("BB:BB:BB:BB:BB:02")
        assertEquals("Sony WH-1000XM5", card()?.title)
        // Every decision is logged with the candidate list.
        assertTrue(AppLogRecorder.lastEntryForTest()!!.contains("Sony WH-1000XM5"))
    }

    @Test
    fun preferredAddressThatDisappearsFallsBackToAConnectedDevice() {
        remoteDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(audioRoute("AA:AA:AA:AA:AA:01"))
        val monitor = monitor()

        monitor.prefer("BB:BB:BB:BB:BB:02")
        assertEquals("Galaxy Buds Pro", card()?.title)
    }

    @Test
    fun cardClearsOnlyWhenNoCountableDeviceRemains() {
        remoteDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(audioRoute("AA:AA:AA:AA:AA:01"))
        val monitor = monitor()
        monitor.refreshNow()
        assertEquals("Galaxy Buds Pro", card()?.title)

        connected = emptyList()
        monitor.refreshNow()
        assertNull(card())
        assertTrue(AppLogRecorder.lastEntryForTest()!!.contains("card cleared"))
    }

    @Test
    fun ignoredDevicesNeverOwnTheCard() {
        wearableRemoteDevice("CC:CC:CC:CC:CC:03", "Galaxy Watch 7")
        remoteDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(audioRoute("CC:CC:CC:CC:CC:03"), audioRoute("AA:AA:AA:AA:AA:01"))
        val monitor = monitor()

        monitor.refreshNow()
        assertEquals("Galaxy Buds Pro", card()?.title)
    }

    /** Watches and trackers hold a GATT link around the clock — GATT devices must never reach the card. */
    @Test
    fun gattOnlyDevicesNeverOwnTheCard() {
        every { manager.getConnectedDevices(BluetoothProfile.GATT) } returns listOf(mockk())
        every { manager.getConnectedDevices(BluetoothProfile.A2DP) } returns emptyList()
        every { manager.getConnectedDevices(BluetoothProfile.HEADSET) } returns emptyList()
        val monitor = monitor()

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

    /** DataStore delivers settings async: unloaded ticks decide nothing, but log the wait once. */
    @Test
    fun ticksStaySilentWhileSettingsAreNotLoadedYet() {
        remoteDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(audioRoute("AA:AA:AA:AA:AA:01"))
        val monitor = BluetoothConnectionMonitor(
            context,
            CoroutineScope(Dispatchers.Unconfined),
            repo,
            { SmartIslandSettings.Default },
            monitorSdkInt
        )

        monitor.refreshNow()
        assertNull(card())
        assertEquals(1, AppLogRecorder.getLogCount())
        assertTrue(AppLogRecorder.lastEntryForTest()!!.contains("settings not loaded"))
    }

    /** Devices connected but every one filtered out: the log must say what was seen, not stay silent. */
    @Test
    fun connectedButEverythingFilteredLogsWhatItSaw() {
        wearableRemoteDevice("CC:CC:CC:CC:CC:03", "Galaxy Watch 7")
        connected = listOf(audioRoute("CC:CC:CC:CC:CC:03"))
        val monitor = monitor()

        monitor.refreshNow()
        assertNull(card())
        val logged = AppLogRecorder.lastEntryForTest()
        assertTrue(logged!!.contains("none survived the filter"))
        assertTrue(logged.contains("Galaxy Watch 7"))

        // The same state does not repeat on every poll.
        monitor.refreshNow()
        assertEquals(1, AppLogRecorder.getLogCount())
    }

    /** Island or event switch off: no card, and one line per process saying which gate is closed. */
    @Test
    fun disabledBluetoothEventLogsOnceAndShowsNoCard() {
        remoteDevice("AA:AA:AA:AA:AA:01", "Galaxy Buds Pro")
        connected = listOf(audioRoute("AA:AA:AA:AA:AA:01"))
        val offSettings = settings.copy(disabledEvents = setOf(com.jero009.essentialisland.model.IslandEvent.BLUETOOTH.id))
        val monitor = monitor { offSettings }

        monitor.refreshNow()
        monitor.refreshNow()
        assertNull(card())
        assertEquals(1, AppLogRecorder.getLogCount())
        assertTrue(AppLogRecorder.lastEntryForTest()!!.contains("Bluetooth card off"))
    }

    /** The old per-profile queries failed in silence — an audio-side enumeration failure must be logged. */
    @Test
    fun enumerationFailureIsLoggedInsteadOfSwallowed() {
        every { audioManager.getDevices(any()) } throws SecurityException("Need BLUETOOTH_CONNECT")
        val monitor = monitor()

        monitor.refreshNow()
        assertNull(card())
        val logged = AppLogRecorder.lastEntryForTest()
        assertTrue(logged!!.contains("enumerating audio devices failed"))
        assertTrue(logged.contains("SecurityException"))
    }

    /** Routes that resolve to nothing (no remote device for the address) are logged, never posted silently. */
    @Test
    fun unresolvedRoutesAreLoggedInsteadOfPosted() {
        connected = listOf(audioRoute("EE:EE:EE:EE:EE:09"))
        every { adapter.getRemoteDevice(any<String>()) } returns null
        val monitor = monitor()

        monitor.refreshNow()
        assertNull(card())
        assertTrue(AppLogRecorder.lastEntryForTest()!!.contains("none resolved to a device"))
    }
}