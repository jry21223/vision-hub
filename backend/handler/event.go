package handler

import (
	"github.com/example/visionhub-backend/service"
	"github.com/gofiber/fiber/v2"
)

// EventHandler handles device event reporting (fall, etc.).
type EventHandler struct {
	publisher *service.EventPublisher
}

func NewEventHandler(publisher *service.EventPublisher) *EventHandler {
	return &EventHandler{publisher: publisher}
}

// reportFallRequest is the expected JSON body for POST /api/v1/event/report.
type reportFallRequest struct {
	DeviceID     string  `json:"device_id"`
	IMUMagnitude float64 `json:"imu_magnitude"`
	Latitude     float64 `json:"latitude"`
	Longitude    float64 `json:"longitude"`
}

// ReportFall godoc
//
//	POST /api/v1/event/report
//	Body: { "device_id": "...", "imu_magnitude": 9.8, "latitude": 0.0, "longitude": 0.0 }
//
// Does NOT write to the database directly.
// Publishes a fall event to Kafka; the consumer goroutine persists it asynchronously.
func (h *EventHandler) ReportFall(c *fiber.Ctx) error {
	var req reportFallRequest
	if err := c.BodyParser(&req); err != nil {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "invalid JSON body: " + err.Error(),
		})
	}
	if req.DeviceID == "" {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "device_id is required",
		})
	}
	if req.IMUMagnitude <= 0 {
		return c.Status(fiber.StatusBadRequest).JSON(fiber.Map{
			"success": false,
			"error":   "imu_magnitude must be positive",
		})
	}

	detail := service.FallDetail{
		IMUMagnitude: req.IMUMagnitude,
		Latitude:     req.Latitude,
		Longitude:    req.Longitude,
	}

	// Fall events must be reliably enqueued — propagate Kafka errors to the client
	// so the Android app can retry if the broker is temporarily unreachable.
	if err := h.publisher.PublishFall(c.Context(), req.DeviceID, detail); err != nil {
		return c.Status(fiber.StatusServiceUnavailable).JSON(fiber.Map{
			"success": false,
			"error":   "event queue unavailable, please retry: " + err.Error(),
		})
	}

	return c.JSON(fiber.Map{
		"success": true,
		"message": "fall event queued",
	})
}
