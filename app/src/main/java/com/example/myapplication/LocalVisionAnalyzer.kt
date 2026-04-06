package com.example.myapplication

class LocalVisionAnalyzer {
    fun analyze(jpegBytes: ByteArray): LocalVisionState {
        if (!isJpegFrame(jpegBytes)) {
            return LocalVisionState(
                status = LocalVisionStatus.ERROR,
                summary = "图像帧格式无效",
            )
        }

        return LocalVisionState(
            status = LocalVisionStatus.FRAME_ANALYZED,
            summary = "最近帧大小 ${jpegBytes.size} 字节",
        )
    }

    private fun isJpegFrame(bytes: ByteArray): Boolean {
        return bytes.size >= MINIMUM_JPEG_SIZE &&
            bytes[0] == JPEG_MARKER_PREFIX &&
            bytes[1] == JPEG_SOI_SUFFIX &&
            bytes[bytes.lastIndex - 1] == JPEG_MARKER_PREFIX &&
            bytes[bytes.lastIndex] == JPEG_EOI_SUFFIX
    }

    private companion object {
        private const val MINIMUM_JPEG_SIZE = 4
        private const val JPEG_MARKER_PREFIX: Byte = 0xFF.toByte()
        private const val JPEG_SOI_SUFFIX: Byte = 0xD8.toByte()
        private const val JPEG_EOI_SUFFIX: Byte = 0xD9.toByte()
    }
}
