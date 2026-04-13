package com.example.myapplication

import java.io.ByteArrayOutputStream
import java.io.InputStream

class VisionStreamDecoder {
    fun decode(
        input: InputStream,
        onSensorPacket: (SensorPacket) -> Unit,
        onImageFrame: (ByteArray) -> Unit,
    ) {
        val lineBuffer = ByteArrayOutputStream()
        var currentByte = input.read()

        while (currentByte != END_OF_STREAM) {
            if (isJpegStart(currentByte, input)) {
                if (lineBuffer.size() > 0) {
                    emitJsonFrame(lineBuffer, onSensorPacket)
                }
                onImageFrame(readJpegFrame(input))
                currentByte = input.read()
                continue
            }

            if (currentByte == NEW_LINE) {
                emitJsonFrame(lineBuffer, onSensorPacket)
            } else if (currentByte != CARRIAGE_RETURN) {
                lineBuffer.write(currentByte)
            }
            currentByte = input.read()
        }

        emitJsonFrame(lineBuffer, onSensorPacket)
    }

    private fun emitJsonFrame(
        lineBuffer: ByteArrayOutputStream,
        onSensorPacket: (SensorPacket) -> Unit,
    ) {
        val frame = lineBuffer.toString(Charsets.UTF_8.name()).trim()
        lineBuffer.reset()
        if (frame.isEmpty()) {
            return
        }

        onSensorPacket(parseSensorPacket(frame))
    }

    private fun parseSensorPacket(frame: String): SensorPacket {
        return SensorPacket(
            radarDist = extractInt(frame, "radar_dist"),
            imu = ImuReading(
                ax = extractDouble(frame, "ax"),
                ay = extractDouble(frame, "ay"),
                az = extractDouble(frame, "az"),
            ),
            btnA = extractInt(frame, "btn_a"),
            btnB = extractInt(frame, "btn_b"),
            batteryPct = extractIntOrNull(frame, "battery_pct"),
        )
    }

    private fun extractInt(frame: String, key: String): Int {
        return extractValue(frame, key).toInt()
    }

    private fun extractDouble(frame: String, key: String): Double {
        return extractValue(frame, key).toDouble()
    }

    private fun extractIntOrNull(frame: String, key: String): Int? {
        return Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(frame)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractValue(frame: String, key: String): String {
        val match = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)").find(frame)
        return requireNotNull(match?.groupValues?.get(1)) {
            "Missing key: $key"
        }
    }

    private fun isJpegStart(firstByte: Int, input: InputStream): Boolean {
        if (firstByte != JPEG_MARKER_PREFIX) {
            return false
        }

        input.mark(1)
        val secondByte = input.read()
        if (secondByte == JPEG_SOI_SUFFIX) {
            return true
        }

        input.reset()
        return false
    }

    private fun readJpegFrame(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(JPEG_MARKER_PREFIX)
        output.write(JPEG_SOI_SUFFIX)

        var previousByte = JPEG_SOI_SUFFIX
        var currentByte = input.read()
        while (currentByte != END_OF_STREAM) {
            output.write(currentByte)
            if (previousByte == JPEG_MARKER_PREFIX && currentByte == JPEG_EOI_SUFFIX) {
                break
            }
            previousByte = currentByte
            currentByte = input.read()
        }

        return output.toByteArray()
    }

    private companion object {
        private const val END_OF_STREAM = -1
        private const val JPEG_MARKER_PREFIX = 0xFF
        private const val JPEG_SOI_SUFFIX = 0xD8
        private const val JPEG_EOI_SUFFIX = 0xD9
        private const val NEW_LINE = '\n'.code
        private const val CARRIAGE_RETURN = '\r'.code
    }
}
