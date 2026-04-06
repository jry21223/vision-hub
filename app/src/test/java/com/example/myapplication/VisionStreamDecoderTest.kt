package com.example.myapplication

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

    private fun sensorJson(radarDist: Int = 120, btnA: Int = 0, btnB: Int = 1): String {
        return """{"radar_dist":$radarDist,"imu":{"ax":0.1,"ay":0.5,"az":9.8},"btn_a":$btnA,"btn_b":$btnB}"""
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
