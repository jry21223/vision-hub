package infra

import (
	"log"
	"sync"
	"time"

	"github.com/example/visionhub-backend/model"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var (
	pgOnce sync.Once
	pgDB   *gorm.DB
)

// DB returns the singleton GORM *DB, initializing it on the first call.
func DB(dsn string) *gorm.DB {
	pgOnce.Do(func() {
		pgDB = mustConnectPostgres(dsn)
	})
	return pgDB
}

func mustConnectPostgres(dsn string) *gorm.DB {
	const (
		maxRetries = 5
		retryDelay = 3 * time.Second
	)

	gormCfg := &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	}

	var (
		db  *gorm.DB
		err error
	)
	for attempt := 1; attempt <= maxRetries; attempt++ {
		db, err = gorm.Open(postgres.Open(dsn), gormCfg)
		if err == nil {
			break
		}
		log.Printf("[postgres] connection attempt %d/%d failed: %v", attempt, maxRetries, err)
		if attempt < maxRetries {
			time.Sleep(retryDelay)
		}
	}
	if err != nil {
		log.Fatalf("[postgres] could not connect after %d attempts: %v", maxRetries, err)
	}

	// Configure the underlying connection pool.
	sqlDB, err := db.DB()
	if err != nil {
		log.Fatalf("[postgres] failed to get sql.DB from gorm: %v", err)
	}
	sqlDB.SetMaxOpenConns(25)
	sqlDB.SetMaxIdleConns(10)
	sqlDB.SetConnMaxLifetime(30 * time.Minute)

	// Auto-migrate registered models.
	if err = db.AutoMigrate(&model.Device{}, &model.EventLog{}); err != nil {
		log.Fatalf("[postgres] auto-migrate failed: %v", err)
	}

	log.Println("[postgres] connected and migrated")
	return db
}
