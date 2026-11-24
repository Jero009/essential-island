/*
 * Essential Island (2026)
 * Based on SmartIsland © Animesh Gupta — github.com/agupta07505 (GPL-3.0)
 * Do not remove or alter this notice.
 */

package com.jero009.essentialisland.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Nothing "NT LINK" ANC control over Bluetooth Classic RFCOMM.
 *
 * Protocol (reverse-engineered by the community — Gadgetbridge, nada, nothingctl; all GPL-compatible
 * sources): frames are
 *
 *   0x55 | control LE16 | command LE16 | length LE16 | opId | payload[len] | crc16 LE16
 *
 * control is 0x0160 (CRC present, TWS device type), crc16 is CRC16-Modbus (init 0xFFFF, reflected
 * poly 0xA001) over every byte before it. Replies carry the request command with bit 15 cleared.
 *
 * ANC: read 0xC01E (reply payload `01 <mode> 00 02 <last> 00`), write 0xF00F payload `01 <mode> 00`
 * (acked by 0x700F, plus an unsolicited 0xE003 `01 <mode> 00` when the mode changes).
 * Modes: 1–3 ANC levels, 4 adaptive, 5 off, 7 transparency.
 */
object NothingAnc {
    val SERVICE_UUID: UUID = UUID.fromString("aeac4a03-dff5-498f-843a-34487cf133eb")

    const val MODE_ANC = 1
    const val MODE_OFF = 5
    const val MODE_TRANSPARENCY = 7
    const val MODE_ADAPTIVE = 4

    /** The three mode circles in display order (Nothing X layout). */
    val ancModes = listOf(
        MODE_ANC to "Noise Cancellation",
        MODE_TRANSPARENCY to "Transparency",
        MODE_OFF to "Off"
    )

    /** The four ANC level segments in display order; only shown while the mode is active. */
    val ancLevels = listOf(1 to "Low", 2 to "Mid", 3 to "High", MODE_ADAPTIVE to "Adaptive")

    /** True when the mode keeps the level segments visible (the ANC family). */
    fun isAncActive(mode: Int?): Boolean = mode != null && mode in 1..MODE_ADAPTIVE

    /** Label for one of the three mode circles, or null for an unknown mode. */
    fun ancModeLabel(mode: Int?): String? = ancModes.firstOrNull { it.first == mode }?.second

    /**
     * The live state line under the section title: "On · Low/Mid/High/Adaptive", "Transparency", "Off".
     * Null for unknown modes (and for null) — the panel hides those anyway.
     */
    fun ancStateLabel(mode: Int?): String? = when {
        mode == null -> null
        mode in 1..3 -> "On · " + (ancLevels.firstOrNull { it.first == mode }?.second ?: "")
        mode == MODE_ADAPTIVE -> "On · Adaptive"
        mode == MODE_TRANSPARENCY -> "Transparency"
        mode == MODE_OFF -> "Off"
        else -> null
    }

    private val opId = AtomicInteger(0)

    /** True when the vendor protocol applies (Nothing/CMF family names). */
    fun isSupportedDevice(name: String?): Boolean {
        val lower = (name ?: "").lowercase()
        return lower.contains("nothing") || lower.contains("cmf ")
    }

    /** True for the CMF family, whose presets go through the listening-mode commands instead of the EQ ones. */
    fun isCmfDevice(name: String?): Boolean = (name ?: "").lowercase().contains("cmf")

    // EQ presets ride the vendor protocol: the Nothing family reads 0xC01F / writes 0xF010, CMF models switch the
    // same presets through the listening-mode commands 0xC050 / 0xF01D. Values from the community protocol tables
    // (nada PROTOCOL.md, swift-nothing-ear, nothingctl — all verified against hardware).
    internal const val READ_EQ = 0xC01F
    internal const val WRITE_EQ = 0xF010
    internal const val READ_LISTENING_MODE = 0xC050
    internal const val WRITE_LISTENING_MODE = 0xF01D
    internal const val READ_IN_EAR = 0xC00E
    internal const val WRITE_IN_EAR = 0xF004

    /** The device's EQ presets in cycling order: (raw value, label). CMF models use the listening-mode set. */
    fun eqPresets(name: String?): List<Pair<Int, String>> =
        if (isCmfDevice(name)) listOf(
            0x00 to "Balanced", 0x01 to "Rock", 0x02 to "Electronic", 0x03 to "Pop",
            0x04 to "Vocals", 0x05 to "Classical", 0x06 to "Custom"
        ) else listOf(
            0x00 to "Balanced", 0x01 to "Voice", 0x02 to "More Treble", 0x03 to "More Bass",
            0x05 to "Custom", 0x06 to "New Voice", 0x07 to "New Instrument"
        )

    /** The current preset byte from an EQ / listening-mode reply: `[?, preset]`, or `[preset]` on some models. */
    fun presetFromPayload(payload: ByteArray): Int? = when {
        payload.size > 1 -> payload[1].toInt() and 0xFF
        payload.size == 1 -> payload[0].toInt() and 0xFF
        else -> null
    }

    /** Label for the preset byte within the device's family, or null when the device reports an unknown value. */
    fun eqLabel(name: String?, preset: Int): String? =
        eqPresets(name).firstOrNull { it.first == preset }?.second

    /** The next preset after [preset] in the family's cycling order (an unknown current value starts at Balanced). */
    fun nextEqPreset(name: String?, preset: Int): Int {
        val presets = eqPresets(name)
        val index = presets.indexOfFirst { it.first == preset }
        return presets[(index + 1).mod(presets.size)].first
    }

    /**
     * In-ear detection from the `0x400E` report `[count, (featureType, on)*]`: feature type 0x01 is the wear
     * detection switch. Null when the report carries no such feature (the model has none).
     */
    fun inEarFromPayload(payload: ByteArray): Boolean? {
        if (payload.isEmpty()) return null
        val count = minOf(payload[0].toInt() and 0xFF, (payload.size - 1) / 2)
        for (i in 0 until count) {
            if (payload[1 + i * 2].toInt() == 0x01) return payload[2 + i * 2].toInt() != 0
        }
        return null
    }

    fun crc16(data: ByteArray, from: Int = 0, to: Int = data.size): Int {
        var crc = 0xFFFF
        for (i in from until to) {
            crc = crc xor (data[i].toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc shr 1) xor 0xA001 else crc shr 1
            }
        }
        return crc and 0xFFFF
    }

    fun buildFrame(cmd: Int, payload: ByteArray = ByteArray(0), fsn: Int = nextOpId()): ByteArray {
        val out = ByteArray(10 + payload.size)
        var i = 0
        out[i++] = 0x55
        out[i++] = 0x60 // control LE16 = 0x0160 (CRC present, device type TWS)
        out[i++] = 0x01
        out[i++] = (cmd and 0xFF).toByte()
        out[i++] = ((cmd shr 8) and 0xFF).toByte()
        out[i++] = (payload.size and 0xFF).toByte()
        out[i++] = ((payload.size shr 8) and 0xFF).toByte()
        out[i++] = (fsn and 0xFF).toByte()
        payload.copyInto(out, i)
        i += payload.size
        val crc = crc16(out, 0, i)
        out[i++] = (crc and 0xFF).toByte()
        out[i] = ((crc shr 8) and 0xFF).toByte()
        return out
    }

    data class Frame(val cmd: Int, val payload: ByteArray)

    /** Parses one frame starting at [start]; returns the frame and the next read offset, or null. */
    fun parse(buf: ByteArray, start: Int, end: Int): Pair<Frame, Int>? {
        if (start + 8 > end || buf[start] != 0x55.toByte()) return null
        val len = (buf[start + 5].toInt() and 0xFF) or ((buf[start + 6].toInt() and 0xFF) shl 8)
        val total = 8 + len + 2
        if (start + total > end) return null
        val crcAt = start + 8 + len
        val expected = (buf[crcAt].toInt() and 0xFF) or ((buf[crcAt + 1].toInt() and 0xFF) shl 8)
        if (crc16(buf, start, crcAt) != expected) return null
        val cmd = (buf[start + 3].toInt() and 0xFF) or ((buf[start + 4].toInt() and 0xFF) shl 8)
        return Frame(cmd, buf.copyOfRange(start + 7 + 1, crcAt)) to (start + total)
    }

    /** Best-effort current ANC mode, read straight off the socket bytes ([readAnc] handles the loop). */
    fun modeFromPayload(payload: ByteArray, cmd: Int): Int? {
        // 0x401E reply: `01 <mode> 00 02 <last> 00`; 0xE003 push: `01 <mode> 00`
        if (payload.isEmpty()) return null
        return when (cmd) {
            0x401E, 0xE003 -> payload.getOrNull(1)?.toInt()?.and(0xFF)
            else -> null
        }
    }

    private fun nextOpId(): Int = opId.incrementAndGet() and 0x7F

    /**
     * The connected audio device matching [name] (A2DP/headset), else any connected audio device.
     * Null when BLUETOOTH_CONNECT is not granted or nothing audio is connected.
     */
    @SuppressLint("MissingPermission")
    fun findConnectedDevice(context: Context, name: String): BluetoothDevice? {
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return null
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return null
        val connected = sequenceOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)
            .flatMap { profile -> runCatching { manager.getConnectedDevices(profile) }.getOrDefault(emptyList()) }
            .distinctBy { it.address }
        return connected.firstOrNull { runCatching { it.name }.getOrNull() == name }
            ?: connected.firstOrNull()
    }
}

/** Blocking RFCOMM client for one earbuds/headphones device; use from Dispatchers.IO only. */
@SuppressLint("MissingPermission")
class NothingAncClient(private val device: BluetoothDevice) {
    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private val lock = Any()

    fun connect(): Boolean = try {
        val s = device.createRfcommSocketToServiceRecord(NothingAnc.SERVICE_UUID)
        s.connect()
        socket = s
        input = s.inputStream
        output = s.outputStream
        true
    } catch (_: Exception) {
        runCatching { socket?.close() }
        socket = null
        false
    }

    fun readAnc(): ByteArray? = transact(0xC01E, ByteArray(0))

    fun writeAnc(mode: Int): Boolean =
        transact(0xF00F, byteArrayOf(0x01, mode.toByte(), 0x00)) != null

    /** Reads the current EQ / listening-mode preset; [cmf] selects the listening-mode commands. */
    fun readEqPreset(cmf: Boolean): ByteArray? =
        transact(if (cmf) NothingAnc.READ_LISTENING_MODE else NothingAnc.READ_EQ, ByteArray(0))

    /** Applies the preset byte [value]; [cmf] selects the listening-mode commands. */
    fun writeEqPreset(value: Int, cmf: Boolean): Boolean =
        transact(if (cmf) NothingAnc.WRITE_LISTENING_MODE else NothingAnc.WRITE_EQ, byteArrayOf(value.toByte(), 0x00)) != null

    fun readInEar(): ByteArray? = transact(NothingAnc.READ_IN_EAR, ByteArray(0))

    fun writeInEar(on: Boolean): Boolean =
        transact(NothingAnc.WRITE_IN_EAR, byteArrayOf(0x01, 0x01, if (on) 0x01 else 0x00)) != null

    fun close() {
        synchronized(lock) {
            runCatching { socket?.close() }
            socket = null
            input = null
            output = null
        }
    }

    /** Writes the frame and reads until the matching reply arrives or the deadline passes. */
    private fun transact(cmd: Int, payload: ByteArray): ByteArray? = synchronized(lock) {
        val out = input ?: return null
        val sink = output ?: return null
        val deadline = System.currentTimeMillis() + 2000L
        return try {
            sink.write(NothingAnc.buildFrame(cmd, payload))
            sink.flush()
            val buffer = ByteArray(512)
            var filled = 0
            val wantReply = cmd and 0x7FFF
            while (System.currentTimeMillis() < deadline) {
                val available = runCatching { out.available() }.getOrDefault(0)
                if (available > 0) {
                    filled += runCatching { out.read(buffer, filled, minOf(available, buffer.size - filled)) }
                        .getOrDefault(-1)
                    if (filled <= 0) return null
                    var scanStart = 0
                    while (scanStart < filled) {
                        val parsed = NothingAnc.parse(buffer, scanStart, filled) ?: break
                        val (frame, next) = parsed
                        scanStart = next
                        if (frame.cmd == wantReply) return frame.payload
                    }
                } else {
                    Thread.sleep(20)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
