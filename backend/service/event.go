package service

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/example/visionhub-backend/model"
	"github.com/segmentio/kafka-go"
)

// EventPayload is the canonical message schema written to Kafka.
// The consumer deserializes this and persists it to EventLog.
type EventPayload struct {
	DeviceID  string          `json:"device_id"`
	EventType model.EventType `json:"event_type"`
	Detail    any             `json:"detail"`
	Timestamp time.Time       `json:"timestamp"`
}

// MedicineDetail is the Detail field payload for medicine recognition events.
type MedicineDetail struct {
	OCRText string `json:"ocr_text"`
	TTSText string `json:"tts_text"`
	Cached  bool   `json:"cached"`
}

// FallDetail is the Detail field payload for fall events.
type FallDetail struct {
	IMUMagnitude float64 `json:"imu_magnitude"`
	Latitude     float64 `json:"latitude,omitempty"`
	Longitude    float64 `json:"longitude,omitempty"`
}

// EventPublisher wraps the Kafka writer and exposes typed publish helpers.
type EventPublisher struct {
	writer *kafka.Writer
}

func NewEventPublisher(w *kafka.Writer) *EventPublisher {
	return &EventPublisher{writer: w}
}

// PublishMedicine sends a medicine recognition event to Kafka (best-effort).
// Errors are logged but never propagated — don't block the HTTP response.
func (p *EventPublisher) PublishMedicine(ctx context.Context, deviceID, ocrText, ttsText string, cached bool) {
	payload := EventPayload{
		DeviceID:  deviceID,
		EventType: model.EventTypeMedicine,
		Detail: MedicineDetail{
			OCRText: ocrText,
			TTSText: ttsText,
			Cached:  cached,
		},
		Timestamp: time.Now().UTC(),
	}
	p.publish(ctx, payload)
}

// PublishFall sends a fall event to Kafka.
// Falls are high-priority; the caller should decide whether to propagate errors.
func (p *EventPublisher) PublishFall(ctx context.Context, deviceID string, detail FallDetail) error {
	payload := EventPayload{
		DeviceID:  deviceID,
		EventType: model.EventTypeFall,
		Detail:    detail,
		Timestamp: time.Now().UTC(),
	}
	return p.publish(ctx, payload)
}

func (p *EventPublisher) publish(ctx context.Context, payload EventPayload) error {
	data, err := json.Marshal(payload)
	if err != nil {
		return fmt.Errorf("event marshal: %w", err)
	}

	msg := kafka.Message{
		Key:   []byte(payload.DeviceID), // partition by device for ordered delivery
		Value: data,
	}

	if err := p.writer.WriteMessages(ctx, msg); err != nil {
		log.Printf("[event-publisher] kafka write error (type=%s device=%s): %v",
			payload.EventType, payload.DeviceID, err)
		return fmt.Errorf("kafka write: %w", err)
	}

	log.Printf("[event-publisher] published type=%s device=%s", payload.EventType, payload.DeviceID)
	return nil
}
