/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ignore rules keep watches, bands and peripherals off the Bluetooth connection card. */
class BtDeviceClassifierTest {

    private fun classed(major: Int?, cls: Int?): BluetoothDevice {
        val device = mockk<BluetoothDevice>()
        if (major == null) {
            every { device.bluetoothClass } returns null
        } else {
            val btClass = mockk<BluetoothClass>()
            every { btClass.majorDeviceClass } returns (major ?: 0)
            every { btClass.deviceClass } returns (cls ?: 0)
            every { device.bluetoothClass } returns btClass
        }
        return device
    }

    @Test
    fun watchNamedDeviceIsIgnoredEvenWhenItsClassSaysAudioVideo() {
        // Several watches carry speakers, so their Bluetooth class is AUDIO_VIDEO — the name must win.
        val watch = classed(BluetoothClass.Device.Major.AUDIO_VIDEO, BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
        assertTrue(BtDeviceClassifier.isIgnoredDevice(watch, "Galaxy Watch 7"))
        assertTrue(BtDeviceClassifier.isIgnoredDevice(watch, "Mi Band 8"))
        assertTrue(BtDeviceClassifier.isIgnoredDevice(watch, "Wear OS device"))
    }

    @Test
    fun leOnlyDeviceWithoutAClassIsJudgedByName() {
        val bleOnly = classed(null, null)
        assertTrue(BtDeviceClassifier.isIgnoredDevice(bleOnly, "Smart Band 9"))
        assertFalse(BtDeviceClassifier.isIgnoredDevice(bleOnly, "Bluetooth Device"))
    }

    @Test
    fun realAudioDevicesAreNeverIgnored() {
        val headphones = classed(BluetoothClass.Device.Major.AUDIO_VIDEO, BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
        assertFalse(BtDeviceClassifier.isIgnoredDevice(headphones, "Sony WH-1000XM5"))
        assertFalse(BtDeviceClassifier.isIgnoredDevice(headphones, "Galaxy Buds Pro"))
        assertFalse(BtDeviceClassifier.isIgnoredDevice(headphones, "Nothing Ear (2)"))
    }

    @Test
    fun wearableAndPeripheralClassesAreIgnored() {
        val wearable = classed(BluetoothClass.Device.Major.WEARABLE, BluetoothClass.Device.WEARABLE_WRIST_WATCH)
        assertTrue(BtDeviceClassifier.isIgnoredDevice(wearable, "Unknown gadget"))
        val peripheral = classed(BluetoothClass.Device.Major.PERIPHERAL, 0)
        assertTrue(BtDeviceClassifier.isIgnoredDevice(peripheral, "Unknown gadget"))
    }

    @Test
    fun peripheralNamedDeviceIsIgnored() {
        val audio = classed(BluetoothClass.Device.Major.AUDIO_VIDEO, BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)
        assertTrue(BtDeviceClassifier.isIgnoredDevice(audio, "Logitech Keyboard"))
        assertTrue(BtDeviceClassifier.isIgnoredDevice(audio, "S Pen"))
    }
}
