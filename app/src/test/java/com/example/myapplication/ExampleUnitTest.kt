package com.example.myapplication

import com.example.myapplication.util.connectionStatusText
import com.example.myapplication.util.fallAlertDescription
import com.example.myapplication.util.fallAlertTitle
import com.example.myapplication.util.localVisionStatusText
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `connectionStatusText maps listening state`() {
        assertEquals("连接状态：监听中", connectionStatusText(ConnectionState.LISTENING))
    }

    @Test
    fun `connectionStatusText maps stopped state`() {
        assertEquals("连接状态：已停止", connectionStatusText(ConnectionState.STOPPED))
    }

    @Test
    fun `connectionStatusText maps error state`() {
        assertEquals("连接状态：连接异常", connectionStatusText(ConnectionState.ERROR))
    }

    @Test
    fun `localVisionStatusText uses idle summary`() {
        assertEquals("本地视觉：等待图像帧", localVisionStatusText(LocalVisionState.IDLE))
    }

    @Test
    fun `localVisionStatusText uses state summary`() {
        val state = LocalVisionState(
            status = LocalVisionStatus.FRAME_ANALYZED,
            summary = "最近帧大小 7 字节",
        )

        assertEquals("本地视觉：最近帧大小 7 字节", localVisionStatusText(state))
    }

    @Test
    fun `fallAlertTitle maps emergency state`() {
        assertEquals("正在呼叫紧急联系人", fallAlertTitle(FallAlertState.EMERGENCY_CALLING))
    }

    @Test
    fun `fallAlertTitle maps confirmed state`() {
        assertEquals("已确认跌倒", fallAlertTitle(FallAlertState.FALL_CONFIRMED))
    }

    @Test
    fun `fallAlertDescription maps detecting state`() {
        assertEquals(
            "检测到异常运动，正在确认是否发生跌倒。",
            fallAlertDescription(FallAlertState.DETECTING),
        )
    }

    @Test
    fun `fallAlertDescription maps confirmed state`() {
        assertEquals(
            "已检测到跌倒，请立即查看佩戴者状态。",
            fallAlertDescription(FallAlertState.FALL_CONFIRMED),
        )
    }
}
