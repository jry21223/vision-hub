package com.example.myapplication

import androidx.compose.ui.graphics.Color
import com.example.myapplication.ui.HistoryRecord
import com.example.myapplication.util.connectionStatusText
import com.example.myapplication.util.fallAlertDescription
import com.example.myapplication.util.fallAlertTitle
import com.example.myapplication.util.filterHistoryRecords
import com.example.myapplication.util.obstacleDangerHeadline
import com.example.myapplication.util.obstacleMetrics
import com.example.myapplication.util.deviceMetrics
import com.example.myapplication.util.localVisionStatusText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun `obstacleDangerHeadline uses radar distance when connected`() {
        assertEquals(
            "前方 0.8 米 危险",
            obstacleDangerHeadline(
                connectionState = ConnectionState.CONNECTED,
                fallAlertState = FallAlertState.IDLE,
                radarDistance = 80,
            ),
        )
    }

    @Test
    fun `obstacleDangerHeadline keeps fall alert priority over radar distance`() {
        assertEquals(
            "检测到跌倒风险",
            obstacleDangerHeadline(
                connectionState = ConnectionState.CONNECTED,
                fallAlertState = FallAlertState.FALL_CONFIRMED,
                radarDistance = 80,
            ),
        )
    }

    @Test
    fun `obstacleMetrics uses real latency value`() {
        val metrics = obstacleMetrics(
            connectionState = ConnectionState.CONNECTED,
            fallAlertState = FallAlertState.IDLE,
            batteryPct = 62,
            latencyMs = 37,
        )

        assertEquals("37ms", metrics.first { it.label == "延迟" }.value)
        assertEquals("62%", metrics.first { it.label == "电量" }.value)
    }

    @Test
    fun `deviceMetrics uses placeholder when latency missing`() {
        val metrics = deviceMetrics(
            connectionState = ConnectionState.CONNECTED,
            batteryPct = 51,
            latencyMs = null,
        )

        assertEquals("--", metrics.first { it.label == "响应延迟" }.value)
        assertEquals("51%", metrics.first { it.label == "剩余电量" }.value)
    }

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

    @Test
    fun `filterHistoryRecords returns all records for blank query`() {
        val records = sampleHistoryRecords()

        assertEquals(records, filterHistoryRecords(records, "   "))
    }

    @Test
    fun `filterHistoryRecords matches title detail and time`() {
        val records = sampleHistoryRecords()

        val titleMatches = filterHistoryRecords(records, "药品")
        val detailMatches = filterHistoryRecords(records, "跌倒")
        val timeMatches = filterHistoryRecords(records, "10月24日")

        assertEquals(listOf(records[0]), titleMatches)
        assertEquals(listOf(records[1]), detailMatches)
        assertEquals(listOf(records[1]), timeMatches)
    }

    @Test
    fun `filterHistoryRecords ignores case`() {
        val records = listOf(
            HistoryRecord(
                title = "Medicine Box",
                detail = "Vitamin C",
                time = "Today 10:30",
                accent = Color.Yellow,
            ),
        )

        val result = filterHistoryRecords(records, "medicine")

        assertEquals(records, result)
    }

    @Test
    fun `filterHistoryRecords returns empty list when nothing matches`() {
        val result = filterHistoryRecords(sampleHistoryRecords(), "语音设置")

        assertTrue(result.isEmpty())
    }

    private fun sampleHistoryRecords(): List<HistoryRecord> {
        return listOf(
            HistoryRecord(
                title = "药品识别",
                detail = "识别摘要：阿司匹林",
                time = "今天 10:30",
                accent = Color.Yellow,
            ),
            HistoryRecord(
                title = "跌倒提醒",
                detail = "已检测到跌倒，请立即查看佩戴者状态。",
                time = "10月24日 08:20",
                accent = Color.Red,
            ),
        )
    }
}
