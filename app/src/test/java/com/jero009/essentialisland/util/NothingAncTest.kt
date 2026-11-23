/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NothingAncTest {

    @Test
    fun crc16MatchesTheModbusCheckValue() {
        assertEquals(0x4B37, NothingAnc.crc16("123456789".toByteArray(Charsets.US_ASCII)))
    }

    @Test
    fun buildFrameLayout() {
        val frame = NothingAnc.buildFrame(0xC007, ByteArray(0), fsn = 1)
        assertEquals(10, frame.size)
        assertEquals(0x55.toByte(), frame[0]) // start of frame
        assertEquals(0x60.toByte(), frame[1]) // control low: CRC present, TWS device type
        assertEquals(0x01.toByte(), frame[2]) // control high
        assertEquals(0x07.toByte(), frame[3]) // command low (little-endian)
        assertEquals(0xC0.toByte(), frame[4]) // command high
        assertEquals(0x00.toByte(), frame[5]) // length low
        assertEquals(0x00.toByte(), frame[6]) // length high
        assertEquals(0x01.toByte(), frame[7]) // fsn
        // CRC16-Modbus over the first 8 bytes, little-endian.
        val crc = NothingAnc.crc16(frame, 0, 8)
        assertEquals((crc and 0xFF).toByte(), frame[8])
        assertEquals(((crc shr 8) and 0xFF).toByte(), frame[9])
    }

    @Test
    fun parseRoundTripsABuiltFrame() {
        val payload = byteArrayOf(0x01, 0x04, 0x00)
        val frame = NothingAnc.buildFrame(0xF00F, payload, fsn = 2)
        val (parsed, next) = NothingAnc.parse(frame, 0, frame.size)!!
        assertEquals(0xF00F, parsed.cmd)
        assertTrue(payload.contentEquals(parsed.payload))
        assertEquals(frame.size, next)
    }

    @Test
    fun parseRejectsACorruptCrc() {
        val frame = NothingAnc.buildFrame(0xC01F, byteArrayOf(0x03, 0x00), fsn = 3)
        frame[frame.size - 1] = (frame[frame.size - 1] + 1).toByte()
        assertNull(NothingAnc.parse(frame, 0, frame.size))
    }

    @Test
    fun ancModeParsesBothReplyForms() {
        assertEquals(4, NothingAnc.modeFromPayload(byteArrayOf(0x01, 0x04, 0x00, 0x02, 0x04, 0x00), 0x401E))
        assertEquals(7, NothingAnc.modeFromPayload(byteArrayOf(0x01, 0x07, 0x00), 0xE003))
        assertNull(NothingAnc.modeFromPayload(ByteArray(0), 0x401E))
        assertNull(NothingAnc.modeFromPayload(byteArrayOf(0x01, 0x04, 0x00), 0x4007))
    }

    @Test
    fun eqPresetsFollowTheDeviceFamily() {
        assertEquals("Balanced", NothingAnc.eqLabel("Nothing Ear (2)", 0x00))
        assertEquals("Voice", NothingAnc.eqLabel("Nothing Ear (2)", 0x01))
        assertEquals("More Bass", NothingAnc.eqLabel("Nothing Ear (2)", 0x03))
        assertEquals("Custom", NothingAnc.eqLabel("Nothing Ear (2)", 0x05))
        assertNull(NothingAnc.eqLabel("Nothing Ear (2)", 0x04))
        val cmf = NothingAnc.eqPresets("CMF Buds 2 Pro").map { it.second }
        assertTrue(cmf.containsAll(listOf("Rock", "Pop", "Vocals", "Classical")))
        assertTrue(NothingAnc.isCmfDevice("CMF Buds Pro"))
        assertFalse(NothingAnc.isCmfDevice("Nothing Ear (a)"))
    }

    @Test
    fun nextEqPresetCyclesWithinTheFamily() {
        assertEquals(0x01, NothingAnc.nextEqPreset("Nothing Ear (2)", 0x00))
        assertEquals(0x05, NothingAnc.nextEqPreset("Nothing Ear (2)", 0x03))
        assertEquals(0x00, NothingAnc.nextEqPreset("Nothing Ear (2)", 0x07))
        assertEquals(0x06, NothingAnc.nextEqPreset("CMF Buds 2", 0x05))
        assertEquals(0x00, NothingAnc.nextEqPreset("CMF Buds 2", 0x06))
        assertEquals(0x00, NothingAnc.nextEqPreset("Nothing Ear (2)", 0x42))
    }

    @Test
    fun presetFromPayloadReadsTheSecondByteWhenPresent() {
        assertEquals(0x03, NothingAnc.presetFromPayload(byteArrayOf(0x01, 0x03, 0x00)))
        assertEquals(0x05, NothingAnc.presetFromPayload(byteArrayOf(0x05)))
        assertNull(NothingAnc.presetFromPayload(ByteArray(0)))
    }

    @Test
    fun inEarFromPayloadWalksTheFeaturePairs() {
        assertEquals(true, NothingAnc.inEarFromPayload(byteArrayOf(0x01, 0x01, 0x01)))
        assertEquals(false, NothingAnc.inEarFromPayload(byteArrayOf(0x01, 0x01, 0x00)))
        // A different feature listed first: wear detection is feature type 0x01.
        assertEquals(false, NothingAnc.inEarFromPayload(byteArrayOf(0x02, 0x03, 0x01, 0x01, 0x00)))
        assertNull(NothingAnc.inEarFromPayload(byteArrayOf(0x01, 0x02, 0x01)))
        assertNull(NothingAnc.inEarFromPayload(ByteArray(0)))
    }
}
