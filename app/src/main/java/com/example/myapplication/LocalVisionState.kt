package com.example.myapplication

data class LocalVisionState(
    val status: LocalVisionStatus,
    val summary: String,
) {
    companion object {
        val IDLE = LocalVisionState(
            status = LocalVisionStatus.IDLE,
            summary = "等待图像帧",
        )
        val PROCESSING = LocalVisionState(
            status = LocalVisionStatus.PROCESSING,
            summary = "正在分析最新画面",
        )
    }
}

enum class LocalVisionStatus {
    IDLE,
    PROCESSING,
    FRAME_ANALYZED,
    ERROR,
}
