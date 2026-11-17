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

    private val opId = AtomicInteger(0)

    /** True when the vendor protocol applies (Nothing/CMF family names). */
    fun isSupportedDevice(name: String?): Boolean {
        val lower = (name ?: "").lowercase()
        return lower.contains("nothing") || lower.contains("cmf ")
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
