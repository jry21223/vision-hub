package model

import "gorm.io/gorm"

// DeviceStatus represents the operational state of a registered device.
type DeviceStatus string

const (
	DeviceStatusActive   DeviceStatus = "active"
	DeviceStatusInactive DeviceStatus = "inactive"
)

// Device maps to the "devices" table.
// MAC address is unique per physical device; BoundUserID links to the owner.
type Device struct {
	gorm.Model
	MACAddress string       `gorm:"column:mac_address;type:varchar(17);uniqueIndex;not null"`
	BoundUserID uint        `gorm:"column:bound_user_id;index"`
	Status      DeviceStatus `gorm:"column:status;type:varchar(20);not null;default:'active'"`
}
