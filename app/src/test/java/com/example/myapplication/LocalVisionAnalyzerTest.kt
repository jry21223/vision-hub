package com.example.myapplication

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloInferenceManagerTest {
    @Test
    fun `selectHighestConfidenceDetection prefers strongest medicine box`() {
        val detections = listOf(
            YoloDetection(
                label = MEDICINE_BOX_LABEL,
                confidence = 0.41f,
                left = 12f,
                top = 20f,
                right = 100f,
                bottom = 140f,
            ),
            YoloDetection(
                label = TEXT_REGION_LABEL,
                confidence = 0.95f,
                left = 60f,
                top = 40f,
                right = 180f,
                bottom = 120f,
            ),
            YoloDetection(
                label = MEDICINE_BOX_LABEL,
                confidence = 0.83f,
                left = 40f,
                top = 36f,
                right = 220f,
                bottom = 210f,
            ),
        )

        val detection = selectHighestConfidenceDetection(detections, MEDICINE_BOX_LABEL)

        assertNotNull(detection)
        assertEquals(0.83f, detection?.confidence ?: 0f, 0.0001f)
    }

    @Test
    fun `describeRelativeLocation maps box to left top and near`() {
        val detection = YoloDetection(
            label = MEDICINE_BOX_LABEL,
            confidence = 0.9f,
            left = 0f,
            top = 0f,
            right = 140f,
            bottom = 140f,
        )

        assertEquals(
            "画面左上，距离较近",
            describeRelativeLocation(detection, frameWidth = 320, frameHeight = 320),
        )
    }

    @Test
    fun `cropBoundsForDetection clamps coordinates into source frame`() {
        val detection = YoloDetection(
            label = TEXT_REGION_LABEL,
            confidence = 0.77f,
            left = -18f,
            top = 6f,
            right = 360f,
            bottom = 270f,
        )

        assertEquals(
            CropBounds(left = 0, top = 6, width = 320, height = 234),
            cropBoundsForDetection(detection, sourceWidth = 320, sourceHeight = 240),
        )
    }

    @Test
    fun `buildLocalVisionSummary reports medicine box location and crop success`() {
        val medicineBox = YoloDetection(
            label = MEDICINE_BOX_LABEL,
            confidence = 0.88f,
            left = 130f,
            top = 90f,
            right = 220f,
            bottom = 180f,
        )

        assertEquals(
            "药盒位于画面中央，距离中等，已截取文本区域",
            buildLocalVisionSummary(
                medicineBox = medicineBox,
                frameWidth = 320,
                frameHeight = 320,
                hasTextRegion = true,
                textRegionCropSucceeded = true,
            ),
        )
    }

    @Test
    fun `buildLocalVisionSummary reports missing medicine box`() {
        assertEquals(
            "未检测到药盒，未检测到文本区域",
            buildLocalVisionSummary(
                medicineBox = null,
                frameWidth = 320,
                frameHeight = 320,
                hasTextRegion = false,
                textRegionCropSucceeded = false,
            ),
        )
    }

    @Test
    fun `parseModelClassLabels parses indexed metadata map`() {
        assertEquals(
            listOf(MEDICINE_BOX_LABEL, TEXT_REGION_LABEL),
            parseModelClassLabels("{0: 'Medicine_Box', 1: 'Text_Region'}"),
        )
    }

    @Test
    fun `supportsRequiredClassLabels requires medicine box and text region`() {
        assertTrue(supportsRequiredClassLabels(listOf(MEDICINE_BOX_LABEL, TEXT_REGION_LABEL)))
    }

    @Test
    fun `parseDetections supports row first output with objectness`() {
        val detections = parseDetections(
            rawOutput = arrayOf(
                arrayOf(
                    floatArrayOf(160f, 160f, 80f, 100f, 0.9f, 0.8f, 0.1f),
                    floatArrayOf(220f, 170f, 70f, 90f, 0.7f, 0.2f, 0.95f),
                ),
            ),
            classLabels = listOf(MEDICINE_BOX_LABEL, TEXT_REGION_LABEL),
        )

        assertEquals(2, detections.size)
        assertEquals(MEDICINE_BOX_LABEL, detections[0].label)
        assertEquals(0.72f, detections[0].confidence, 0.0001f)
        assertEquals(TEXT_REGION_LABEL, detections[1].label)
        assertEquals(0.665f, detections[1].confidence, 0.0001f)
    }

    @Test
    fun `parseDetections supports channel first output without objectness`() {
        val detections = parseDetections(
            rawOutput = arrayOf(
                arrayOf(
                    floatArrayOf(120f, 240f),
                    floatArrayOf(100f, 180f),
                    floatArrayOf(60f, 80f),
                    floatArrayOf(50f, 90f),
                    floatArrayOf(0.91f, 0.15f),
                    floatArrayOf(0.05f, 0.88f),
                ),
            ),
            classLabels = listOf(MEDICINE_BOX_LABEL, TEXT_REGION_LABEL),
        )

        assertEquals(2, detections.size)
        assertEquals(MEDICINE_BOX_LABEL, detections[0].label)
        assertEquals(TEXT_REGION_LABEL, detections[1].label)
    }

    @Test
    fun `parseDetections returns empty when labels missing`() {
        assertEquals(
            emptyList<YoloDetection>(),
            parseDetections(
                rawOutput = arrayOf(arrayOf(floatArrayOf(1f, 2f, 3f, 4f, 0.9f))),
                classLabels = emptyList(),
            ),
        )
    }
}
