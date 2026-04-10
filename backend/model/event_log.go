package model

import (
	"time"

	"gorm.io/gorm"
)

// EventType enumerates the kinds of events the platform handles.
type EventType string

const (
	EventTypeFall      EventType = "fall"
	EventTypeMedicine  EventType = "medicine_recognize"
)

// EventLog maps to the "event_logs" table.
// Detail stores arbitrary event-specific JSON (OCR text, LLM response, IMU snapshot, etc.).
type EventLog struct {
	ID        uint           `gorm:"primaryKey;autoIncrement"`
	DeviceID  uint           `gorm:"column:device_id;index;not null"`
	EventType EventType      `gorm:"column:event_type;type:varchar(30);not null"`
	Detail    string         `gorm:"column:detail;type:jsonb"` // JSONB for PostgreSQL query flexibility
	CreatedAt time.Time      `gorm:"column:created_at;autoCreateTime"`
	DeletedAt gorm.DeletedAt `gorm:"index"` // soft-delete support
}
