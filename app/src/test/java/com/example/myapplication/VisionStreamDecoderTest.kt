package com.example.myapplication

import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionStreamDecoderTest {
    private val decoder = VisionStreamDecoder()

    @Test
    fun `decode emits sensor packet from json frame`() {
        val sensorPackets = mutableListOf<SensorPacket>()

        decoder.decode(
            input = sensorJson().byteInputStream(),
            onSensorPacket = { packet -> sensorPackets.add(packet) },
            onImageFrame = { error("Did not expect image frame") },
        )

        assertEquals(1, sensorPackets.size)
        assertEquals(120, sensorPackets.single().radarDist)
        assertEquals(0.1, sensorPackets.single().imu.ax, 0.0)
        assertEquals(0.5, sensorPackets.single().imu.ay, 0.0)
        assertEquals(9.8, sensorPackets.single().imu.az, 0.0)
        assertEquals(0, sensorPackets.single().btnA)
        assertEquals(1, sensorPackets.single().btnB)
    }

    @Test
    fun `decode emits battery percentage when provided`() {
        val sensorPackets = mutableListOf<SensorPacket>()

        decoder.decode(
            input = sensorJson(batteryPct = 76).byteInputStream(),
            onSensorPacket = { packet -> sensorPackets.add(packet) },
            onImageFrame = { error("Did not expect image frame") },
        )

        assertEquals(1, sensorPackets.size)
        assertEquals(76, sensorPackets.single().batteryPct)
    }

    @Test
    fun `decode emits null battery percentage when omitted`() {
        val sensorPackets = mutableListOf<SensorPacket>()

        decoder.decode(
            input = sensorJson().byteInputStream(),
            onSensorPacket = { packet -> sensorPackets.add(packet) },
            onImageFrame = { error("Did not expect image frame") },
        )

        assertEquals(1, sensorPackets.size)
        assertEquals(null, sensorPackets.single().batteryPct)
    }

    @Test
    fun `decode skips malformed json and continues reading later sensor packet`() {
        val sensorPackets = mutableListOf<SensorPacket>()
        val payload = "{bad json}\n".encodeToByteArray() +
            sensorJson(radarDist = 40, btnA = 1, btnB = 0).encodeToByteArray()

        decoder.decode(
            input = payload.inputStream(),
            onSensorPacket = { packet -> sensorPackets.add(packet) },
            onImageFrame = { error("Did not expect image frame") },
        )

        assertEquals(1, sensorPackets.size)
        assertEquals(40, sensorPackets.single().radarDist)
        assertEquals(1, sensorPackets.single().btnA)
        assertEquals(0, sensorPackets.single().btnB)
    }

    @Test
    fun `decode throws when jpeg frame is truncated before end marker`() {
        val truncatedJpeg = byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0x01,
            0x23,
            0x45,
        )

        val error = org.junit.Assert.assertThrows(IOException::class.java) {
            decoder.decode(
                input = truncatedJpeg.inputStream(),
                onSensorPacket = { error("Did not expect sensor frame") },
                onImageFrame = { error("Did not expect image frame callback") },
            )
        }

        assertEquals("Incomplete JPEG frame", error.message)
    }

    @Test
    fun `decode emits jpeg bytes from jpeg frame`() {
        val imageFrames = mutableListOf<ByteArray>()
        val expectedImage = jpegFrame()

        decoder.decode(
            input = expectedImage.inputStream(),
            onSensorPacket = { error("Did not expect sensor frame") },
            onImageFrame = { frame -> imageFrames.add(frame) },
        )

        assertEquals(1, imageFrames.size)
        assertArrayEquals(expectedImage, imageFrames.single())
    }

    @Test
    fun `decode emits interleaved frames in order`() {
        val frameTypes = mutableListOf<String>()
        val sensorPackets = mutableListOf<SensorPacket>()
        val imageFrames = mutableListOf<ByteArray>()
        val expectedImage = jpegFrame()
        val payload = sensorJson().encodeToByteArray() +
            byteArrayOf('\n'.code.toByte(), '\n'.code.toByte()) +
            expectedImage +
            byteArrayOf('\n'.code.toByte()) +
            sensorJson(radarDist = 40, btnA = 1, btnB = 0).encodeToByteArray()

        decoder.decode(
            input = payload.inputStream(),
            onSensorPacket = { packet ->
                frameTypes += "sensor"
                sensorPackets += packet
            },
            onImageFrame = { frame ->
                frameTypes += "image"
                imageFrames += frame
            },
        )

        assertEquals(listOf("sensor", "image", "sensor"), frameTypes)
        assertEquals(listOf(120, 40), sensorPackets.map(SensorPacket::radarDist))
        assertEquals(1, imageFrames.size)
        assertTrue(imageFrames.single().isNotEmpty())
        assertArrayEquals(expectedImage, imageFrames.single())
    }

    private fun sensorJson(
        radarDist: Int = 120,
        btnA: Int = 0,
        btnB: Int = 1,
        batteryPct: Int? = null,
    ): String {
        val batteryField = batteryPct?.let { ",\"battery_pct\":$it" }.orEmpty()
        return """{"radar_dist":$radarDist,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":$btnA,"btn_b":$btnB$batteryField}"""
    }

    private fun jpegFrame(): ByteArray {
        return byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0x01,
            0x23,
            0x45,
            0x67,
            0xFF.toByte(),
            0xD9.toByte(),
        )
    }
}
