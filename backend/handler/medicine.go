package handler

import (
	"github.com/example/visionhub-backend/service"
	"github.com/gofiber/fiber/v2"
)

// MedicineHandler handles requests for the medicine recognition feature.
type MedicineHandler struct {
	svc *service.MedicineService
}

func NewMedicineHandler(svc *service.MedicineService) *MedicineHandler {
	return &MedicineHandler{svc: svc}
}

// recognizeRequest is the expected JSON body for POST /api/v1/recognize/medicine.
type recognizeRequest struct {
	DeviceID    string `json:"device_id"`
	ImageBase64 string `json:"image_base64"`
}

// recognizeResponse is the unified envelope returned to the client.
type recognizeResponse struct {
	Success bool                  `json:"success"`
	Data    *service.MedicineResult `json:"data,omitempty"`
	Error   string                `json:"error,omitempty"`
}

// Recognize godoc
//
//	POST /api/v1/recognize/medicine
//	Body: { "device_id": "...", "image_base64": "..." }
//
// Flow: OCR → MD5 cache key → Redis GET → (miss) LLM → Redis SET → TTS text response.
func (h *MedicineHandler) Recognize(c *fiber.Ctx) error {
	var req recognizeRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(recognizeResponse{
			Error: "invalid JSON body: " + err.Error(),
		})
	}
	if req.DeviceID == "" || req.ImageBase64 == "" {
		return c.Status(fiber.StatusBadRequest).JSON(recognizeResponse{
			Error: "device_id and image_base64 are required",
		})
	}

	result, err := h.svc.Recognize(c.Context(), req.DeviceID, req.ImageBase64)
	if err != nil {
		return c.Status(fiber.StatusInternalServerError).JSON(recognizeResponse{
			Error: err.Error(),
		})
	}

	return c.JSON(recognizeResponse{
		Success: true,
		Data:    result,
	})
}
