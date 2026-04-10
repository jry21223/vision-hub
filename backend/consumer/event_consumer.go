package consumer

import (
	"context"
	"encoding/json"
	"errors"
	"log"

	"github.com/example/visionhub-backend/model"
	"github.com/example/visionhub-backend/service"
	"github.com/segmentio/kafka-go"
	"gorm.io/gorm"
)

// Start launches the Kafka consumer in a background goroutine.
// It stops cleanly when ctx is cancelled (triggered by SIGINT / SIGTERM).
func Start(ctx context.Context, reader *kafka.Reader, db *gorm.DB) {
	go func() {
		log.Println("[consumer] event consumer started")
		defer func() {
			if err := reader.Close(); err != nil {
				log.Printf("[consumer] reader close error: %v", err)
			}
			log.Println("[consumer] event consumer stopped")
		}()

		for {
			msg, err := reader.ReadMessage(ctx)
			if err != nil {
				// Context cancellation is the expected shutdown path — not an error.
				if errors.Is(err, context.Canceled) || errors.Is(err, context.DeadlineExceeded) {
					return
				}
				log.Printf("[consumer] read error: %v", err)
				continue
			}

			if err := handleMessage(db, msg); err != nil {
				log.Printf("[consumer] handle error (offset=%d): %v", msg.Offset, err)
				// Continue consuming — a single bad message must not stall the pipeline.
			}
		}
	}()
}

// handleMessage deserializes one Kafka message and persists it to EventLog.
func handleMessage(db *gorm.DB, msg kafka.Message) error {
	var payload service.EventPayload
	if err := json.Unmarshal(msg.Value, &payload); err != nil {
		return err
	}

	// Re-serialize Detail as a JSON string for the JSONB column.
	detailJSON, err := json.Marshal(payload.Detail)
	if err != nil {
		return err
	}

	record := model.EventLog{
		DeviceID:  deviceIDFromString(payload.DeviceID),
		EventType: payload.EventType,
		Detail:    string(detailJSON),
		CreatedAt: payload.Timestamp,
	}

	if result := db.Create(&record); result.Error != nil {
		return result.Error
	}

	log.Printf("[consumer] persisted event_log id=%d type=%s device=%s",
		record.ID, record.EventType, payload.DeviceID)
	return nil
}

// deviceIDFromString converts a device identifier string to a uint primary key.
// In production this should query the devices table; here we use a placeholder.
// TODO: replace with a Device lookup by MAC address once Device registration is wired.
func deviceIDFromString(_ string) uint {
	return 0 // foreign key relaxed during development
}
