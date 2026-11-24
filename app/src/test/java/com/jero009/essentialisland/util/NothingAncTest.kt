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

    /** The mode write (0xF00F) carries `01 <mode> 00` — the byte layout the panel's taps produce. */
    @Test
    fun ancWriteFrameCarriesTheModePayload() {
        listOf(1, 2, 3, 4, 5, 7).forEach { mode ->
            val payload = byteArrayOf(0x01, mode.toByte(), 0x00)
            val frame = NothingAnc.buildFrame(0xF00F, payload, fsn = 4)
            assertEquals(13, frame.size)
            assertEquals(0x55.toByte(), frame[0])
            assertEquals(0x0F.toByte(), frame[3]) // command LE16 = 0xF00F
            assertEquals(0xF0.toByte(), frame[4])
            assertEquals(0x03.toByte(), frame[5]) // length LE16 = 3
            assertEquals(0x01.toByte(), frame[8])
            assertEquals(mode.toByte(), frame[9])
            assertEquals(0x00.toByte(), frame[10])
            frame[frame.size - 1] = (frame[frame.size - 1] + 1).toByte()
            assertNull(NothingAnc.parse(frame, 0, frame.size))
        }
    }

    /** The panel's live subtitle follows the real mode byte (shown after every write's re-read). */
    @Test
    fun ancPanelStateLabelsFollowTheMode() {
        assertEquals("On · Low", NothingAnc.ancStateLabel(1))
        assertEquals("On · Mid", NothingAnc.ancStateLabel(2))
        assertEquals("On · High", NothingAnc.ancStateLabel(3))
        assertEquals("On · Adaptive", NothingAnc.ancStateLabel(4))
        assertEquals("Transparency", NothingAnc.ancStateLabel(7))
        assertEquals("Off", NothingAnc.ancStateLabel(5))
        assertNull(NothingAnc.ancStateLabel(null))
        assertNull(NothingAnc.ancStateLabel(9))
    }

    /** The three mode circles and four level segments map onto the protocol modes exactly. */
    @Test
    fun ancPanelStructureCoversTheProtocolModes() {
        assertEquals(
            listOf(NothingAnc.MODE_ANC to "Noise Cancellation", NothingAnc.MODE_TRANSPARENCY to "Transparency", NothingAnc.MODE_OFF to "Off"),
            NothingAnc.ancModes
        )
        assertEquals(listOf(1 to "Low", 2 to "Mid", 3 to "High", NothingAnc.MODE_ADAPTIVE to "Adaptive"), NothingAnc.ancLevels)
        listOf(1, 2, 3, 4).forEach { assertTrue(NothingAnc.isAncActive(it)) }
        listOf(5, 7, null, 9).forEach { assertFalse(NothingAnc.isAncActive(it)) }
        assertEquals("Noise Cancellation", NothingAnc.ancModeLabel(1))
        assertEquals("Transparency", NothingAnc.ancModeLabel(7))
        assertEquals("Off", NothingAnc.ancModeLabel(5))
        assertNull(NothingAnc.ancModeLabel(4)) // a level, not a circle mode
        assertNull(NothingAnc.ancModeLabel(null))
    }
}
