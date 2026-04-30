package infra

import (
	"context"
	"log"
	"sync"
	"time"

	"github.com/redis/go-redis/v9"
)

var (
	rdbOnce sync.Once
	rdb     *redis.Client
)

// Redis returns the singleton redis.Client, initializing it on the first call.
func Redis(addr, password string, db int) *redis.Client {
	rdbOnce.Do(func() {
		rdb = mustConnectRedis(addr, password, db)
	})
	return rdb
}

func mustConnectRedis(addr, password string, db int) *redis.Client {
	const (
		maxRetries = 5
		retryDelay = 3 * time.Second
	)

	client := redis.NewClient(&redis.Options{
		Addr:         addr,
		Password:     password,
		DB:           db,
		DialTimeout:  5 * time.Second,
		ReadTimeout:  3 * time.Second,
		WriteTimeout: 3 * time.Second,
		// Built-in retry for transient network errors.
		MaxRetries:      3,
		MinRetryBackoff: 500 * time.Millisecond,
		MaxRetryBackoff: 2 * time.Second,
	})

	ctx := context.Background()
	for attempt := 1; attempt <= maxRetries; attempt++ {
		if err := client.Ping(ctx).Err(); err == nil {
			break
		} else {
			log.Printf("[redis] ping attempt %d/%d failed: %v", attempt, maxRetries, err)
			if attempt == maxRetries {
				log.Fatalf("[redis] could not reach Redis after %d attempts", maxRetries)
			}
			time.Sleep(retryDelay)
		}
	}

	log.Println("[redis] connected")
	return client
}
