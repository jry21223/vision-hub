package config

import (
	"log"
	"os"
	"strconv"

	"github.com/joho/godotenv"
)

// Config holds all runtime configuration loaded from environment variables.
type Config struct {
	AppPort string

	// PostgreSQL
	DatabaseURL string

	// Redis
	RedisAddr     string
	RedisPassword string
	RedisDB       int

	// Kafka
	KafkaBrokers string
	KafkaTopic   string
}

// Load reads .env (if present) then environment variables.
// Missing required variables cause an immediate fatal log.
func Load() *Config {
	if err := godotenv.Load(); err != nil {
		log.Println("[config] no .env file found, reading from process environment")
	}

	redisDB, err := strconv.Atoi(getEnv("REDIS_DB", "0"))
	if err != nil {
		log.Fatalf("[config] REDIS_DB must be an integer: %v", err)
	}

	return &Config{
		AppPort:       getEnv("APP_PORT", "3000"),
		DatabaseURL:   mustGetEnv("DATABASE_URL"),
		RedisAddr:     getEnv("REDIS_ADDR", "localhost:6379"),
		RedisPassword: getEnv("REDIS_PASSWORD", ""),
		RedisDB:       redisDB,
		KafkaBrokers:  getEnv("KAFKA_BROKERS", "localhost:9092"),
		KafkaTopic:    getEnv("KAFKA_TOPIC", "sensor_events"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}

func mustGetEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		log.Fatalf("[config] required environment variable %q is not set", key)
	}
	return v
}
