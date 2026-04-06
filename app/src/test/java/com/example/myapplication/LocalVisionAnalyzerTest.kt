package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalVisionAnalyzerTest {
    private val analyzer = LocalVisionAnalyzer()

    @Test
    fun `analyze returns analyzed state for jpeg frame`() {
        val result = analyzer.analyze(
            byteArrayOf(
                0xFF.toByte(),
                0xD8.toByte(),
                0x01,
                0x23,
                0x45,
                0xFF.toByte(),
                0xD9.toByte(),
            ),
        )

        assertEquals(LocalVisionStatus.FRAME_ANALYZED, result.status)
        assertEquals("最近帧大小 7 字节", result.summary)
    }

    @Test
    fun `analyze returns error state for invalid frame`() {
        val result = analyzer.analyze(byteArrayOf(0x01, 0x02, 0x03, 0x04))

        assertEquals(LocalVisionStatus.ERROR, result.status)
        assertEquals("图像帧格式无效", result.summary)
    }
}
