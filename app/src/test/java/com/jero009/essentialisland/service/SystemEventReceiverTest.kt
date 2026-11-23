/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.service

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.PowerManager
import com.jero009.essentialisland.data.SmartIslandNotificationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemEventReceiverTest {

    @Test
    fun testPowerDisconnectedPostsBriefUnpluggedCard() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)

        val context = mockk<Context>()
        val pm = mockk<PowerManager>(relaxed = true)
        every { pm.isPowerSaveMode } returns false
        every { context.getSystemService(Context.POWER_SERVICE) } returns pm

        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_POWER_DISCONNECTED
        every { intent.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100

        receiver.onReceive(context, intent)

        verify {
            repo.postNotification(
                match {
                    it.key == "system_battery" &&
                        it.category == "battery_plugged_out" &&
                        it.text == "80%"
                },
                autoExpand = true
            )
        }
    }

    @Test
    fun testPowerConnectedPostsPluggedInCardThenNothingElseImmediately() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        fun plugIntent(action: String) = mockk<Intent>().also {
            every { it.action } returns action
            every { it.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
            every { it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 60
            every { it.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
            every { it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns BatteryManager.BATTERY_PLUGGED_AC
            every { it.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING
        }

        receiver.onReceive(context, plugIntent(Intent.ACTION_POWER_CONNECTED))
        // The battery broadcast that follows a plug-in must not replace the plug card.
        receiver.onReceive(context, plugIntent(Intent.ACTION_BATTERY_CHANGED))

        verify(exactly = 1) {
            repo.postNotification(
                match { it.category == "battery_plugged_in" && it.title == "Plugged in" && it.text == "60%" },
                autoExpand = true
            )
        }
        verify(exactly = 0) {
            repo.postNotification(match { it.category == "battery_charging" }, any())
        }
    }

    @Test
    fun testChargeLimitPauseDoesNotCountAsUnplugged() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        fun batteryIntent(plugged: Int, status: Int) = mockk<Intent>().also {
            every { it.action } returns Intent.ACTION_BATTERY_CHANGED
            every { it.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
            every { it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
            every { it.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
            every { it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns plugged
            every { it.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns status
        }

        // Sticky broadcast on registration while charging: adopted silently.
        receiver.onReceive(context, batteryIntent(BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_STATUS_CHARGING))
        // A charge limit pauses charging: still plugged, status NOT_CHARGING.
        receiver.onReceive(context, batteryIntent(BatteryManager.BATTERY_PLUGGED_AC, BatteryManager.BATTERY_STATUS_NOT_CHARGING))

        verify(exactly = 0) {
            repo.postNotification(match { it.category == "battery_plugged_out" }, any())
        }
        verify(exactly = 0) {
            repo.postNotification(any(), autoExpand = true)
        }
    }

    @Test
    fun testBatteryLowPostsLowBatteryNotification() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        
        val context = mockk<Context>()
        val pm = mockk<PowerManager>(relaxed = true)
        every { pm.isPowerSaveMode } returns false
        every { context.getSystemService(Context.POWER_SERVICE) } returns pm

        val intent = mockk<Intent>()
        every { intent.action } returns Intent.ACTION_BATTERY_LOW
        every { intent.hasExtra(BatteryManager.EXTRA_LEVEL) } returns true
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 15
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        
        receiver.onReceive(context, intent)
        
        verify {
            repo.postNotification(
                match { it.key == "system_battery" && it.title == "Low Battery" && it.text == "15%" },
                autoExpand = true
            )
        }
    }

    @Test
    fun testSmartwatchConnectionIgnored() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        val watchDevice = mockk<BluetoothDevice>()
        val bluetoothClass = mockk<BluetoothClass>()
        every { bluetoothClass.majorDeviceClass } returns BluetoothClass.Device.Major.WEARABLE
        every { bluetoothClass.deviceClass } returns BluetoothClass.Device.WEARABLE_WRIST_WATCH
        every { watchDevice.bluetoothClass } returns bluetoothClass
        every { watchDevice.name } returns "Galaxy Watch6"
        every { watchDevice.address } returns "11:22:33:44:55:66"

        val intent = mockk<Intent>()
        every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns watchDevice
        @Suppress("DEPRECATION")
        every { intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns watchDevice
        every { intent.getIntExtra(any(), -1) } returns -1

        receiver.onReceive(context, intent)

        // Smartwatch connection should NOT post any notification or expand the island
        verify(exactly = 0) { repo.postNotification(any(), any()) }
    }

    @Test
    fun testAudioDeviceConnectionAllowedAndDebounced() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        val audioDevice = mockk<BluetoothDevice>()
        val bluetoothClass = mockk<BluetoothClass>()
        every { bluetoothClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { bluetoothClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { audioDevice.bluetoothClass } returns bluetoothClass
        every { audioDevice.name } returns "Galaxy Buds Pro"
        every { audioDevice.address } returns "AA:BB:CC:DD:EE:01"

        val intent = mockk<Intent>()
        every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns audioDevice
        @Suppress("DEPRECATION")
        every { intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns audioDevice
        every { intent.getIntExtra(any(), -1) } returns 85

        // First connection: autoExpand = true ("Galaxy Buds Pro" classifies as earbuds: the label says so)
        receiver.onReceive(context, intent)
        verify(exactly = 1) {
            repo.postNotification(
                match { it.key == "system_bluetooth" && it.title == "Galaxy Buds Pro" && it.text == "Earbuds connected • 85%" },
                autoExpand = true
            )
        }

        // Rapid second connection event for same device: autoExpand = false (debounced)
        receiver.onReceive(context, intent)
        verify(exactly = 1) {
            repo.postNotification(
                match { it.key == "system_bluetooth" },
                autoExpand = false
            )
        }
    }

    @Test
    fun testWearableDisconnectDoesNotDismissActiveAudioDeviceNotification() {
        val repo = mockk<SmartIslandNotificationRepository>(relaxed = true)
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()
        // The disconnect path now re-checks what is still connected before dismissing: the audio device
        // stays connected the whole time, so the stack keeps reporting it until it actually disconnects.
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val manager = mockk<android.bluetooth.BluetoothManager>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager

        val audioDevice = mockk<BluetoothDevice>()
        val audioBtClass = mockk<BluetoothClass>()
        every { audioBtClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { audioBtClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { audioDevice.bluetoothClass } returns audioBtClass
        every { audioDevice.name } returns "Galaxy Buds Pro"
        every { audioDevice.address } returns "AA:BB:CC:DD:EE:01"
        every { manager.getConnectedDevices(any()) } returns listOf(audioDevice)

        // Connect audio device
        val connectIntent = mockk<Intent>()
        every { connectIntent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { connectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns audioDevice
        @Suppress("DEPRECATION")
        every { connectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns audioDevice
        every { connectIntent.getIntExtra(any(), -1) } returns 90
        receiver.onReceive(context, connectIntent)

        // Now an unrelated watch disconnects
        val watchDevice = mockk<BluetoothDevice>()
        every { watchDevice.address } returns "99:88:77:66:55:44"
        val watchDisconnectIntent = mockk<Intent>()
        every { watchDisconnectIntent.action } returns BluetoothDevice.ACTION_ACL_DISCONNECTED
        every { watchDisconnectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns watchDevice
        @Suppress("DEPRECATION")
        every { watchDisconnectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns watchDevice

        receiver.onReceive(context, watchDisconnectIntent)

        // The audio device's system_bluetooth notification must NOT be removed!
        verify(exactly = 0) { repo.removeNotification("system_bluetooth") }

        // Now the actual audio device disconnects
        every { manager.getConnectedDevices(any()) } returns emptyList()
        val audioDisconnectIntent = mockk<Intent>()
        every { audioDisconnectIntent.action } returns BluetoothDevice.ACTION_ACL_DISCONNECTED
        every { audioDisconnectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns audioDevice
        @Suppress("DEPRECATION")
        every { audioDisconnectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns audioDevice

        receiver.onReceive(context, audioDisconnectIntent)

        // Now it SHOULD be removed
        verify(exactly = 1) { repo.removeNotification("system_bluetooth") }
    }

    /** Two audio devices: disconnecting the card's device hands the card to the one still connected. */
    @Test
    fun testDisconnectingOneOfTwoDevicesHandsTheCardToTheRemainingOne() {
        val repo = SmartIslandNotificationRepository()
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()
        every { context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) } returns
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val manager = mockk<android.bluetooth.BluetoothManager>()

        val earbuds = mockk<BluetoothDevice>()
        val earbudsClass = mockk<BluetoothClass>()
        every { earbudsClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { earbudsClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { earbuds.bluetoothClass } returns earbudsClass
        every { earbuds.name } returns "Galaxy Buds Pro"
        every { earbuds.address } returns "AA:AA:AA:AA:AA:01"

        val headphones = mockk<BluetoothDevice>()
        val headphonesClass = mockk<BluetoothClass>()
        every { headphonesClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { headphonesClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { headphones.bluetoothClass } returns headphonesClass
        every { headphones.name } returns "Sony WH-1000XM5"
        every { headphones.address } returns "BB:BB:BB:BB:BB:02"

        fun connectIntentFor(device: BluetoothDevice): Intent = mockk<Intent>().also { intent ->
            every { intent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
            every { intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns device
            @Suppress("DEPRECATION")
            every { intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns device
            every { intent.getIntExtra(any(), -1) } returns -1
        }

        fun disconnectIntentFor(device: BluetoothDevice): Intent = mockk<Intent>().also { intent ->
            every { intent.action } returns BluetoothDevice.ACTION_ACL_DISCONNECTED
            every { intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns device
            @Suppress("DEPRECATION")
            every { intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns device
        }

        // Earbuds connect, then headphones connect: the card follows the newest device.
        every { manager.getConnectedDevices(any()) } returns listOf(headphones, earbuds)
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns manager
        receiver.onReceive(context, connectIntentFor(earbuds))
        receiver.onReceive(context, connectIntentFor(headphones))
        assertEquals("Sony WH-1000XM5", repo.notifications.value.last { it.key == "system_bluetooth" }.title)

        // The card's device (headphones) disconnects: the card switches to the earbuds, it is not dismissed.
        every { manager.getConnectedDevices(any()) } returns listOf(earbuds)
        receiver.onReceive(context, disconnectIntentFor(headphones))
        val card = repo.notifications.value.lastOrNull { it.key == "system_bluetooth" }
        assertEquals("Galaxy Buds Pro", card?.title)
        assertEquals("Earbuds connected", card?.text)

        // The earbuds disconnect too: only now the card goes away.
        every { manager.getConnectedDevices(any()) } returns emptyList()
        receiver.onReceive(context, disconnectIntentFor(earbuds))
        assertEquals(null, repo.notifications.value.find { it.key == "system_bluetooth" })
    }

    /** A battery update keeps the "Earbuds/Headphones connected" prefix instead of flattening it to "Connected". */
    @Test
    fun testBatteryUpdateKeepsTheDeviceLabel() {
        val repo = SmartIslandNotificationRepository()
        val receiver = SystemEventReceiver(repo)
        val context = mockk<Context>()

        val device = mockk<BluetoothDevice>()
        val deviceClass = mockk<BluetoothClass>()
        every { deviceClass.majorDeviceClass } returns BluetoothClass.Device.Major.AUDIO_VIDEO
        every { deviceClass.deviceClass } returns BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
        every { device.bluetoothClass } returns deviceClass
        every { device.name } returns "Galaxy Buds Pro"
        every { device.address } returns "AA:AA:AA:AA:AA:01"

        val connectIntent = mockk<Intent>()
        every { connectIntent.action } returns BluetoothDevice.ACTION_ACL_CONNECTED
        every { connectIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java) } returns device
        @Suppress("DEPRECATION")
        every { connectIntent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) } returns device
        every { connectIntent.getIntExtra(any(), -1) } returns 85
        receiver.onReceive(context, connectIntent)
        assertEquals("Earbuds connected • 85%", repo.notifications.value.last { it.key == "system_bluetooth" }.text)

        val batteryIntent = mockk<Intent>()
        every { batteryIntent.action } returns "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        every { batteryIntent.getIntExtra(any(), -1) } returns 92
        receiver.onReceive(context, batteryIntent)
        assertEquals("Earbuds connected • 92%", repo.notifications.value.last { it.key == "system_bluetooth" }.text)
    }
}
