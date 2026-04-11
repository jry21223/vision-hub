package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gofiber/fiber/v2"
	"github.com/gofiber/fiber/v2/middleware/logger"
	"github.com/gofiber/fiber/v2/middleware/recover"

	"github.com/example/visionhub-backend/config"
	"github.com/example/visionhub-backend/consumer"
	"github.com/example/visionhub-backend/infra"
	"github.com/example/visionhub-backend/route"
)

func main() {
	cfg := config.Load()

	// ── Cancellable root context ───────────────────────────────────────────
	// ctx is passed to the Kafka consumer; cancelling it triggers a clean stop.
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// ── Infrastructure singletons ──────────────────────────────────────────
	db := infra.DB(cfg.DatabaseURL)
	rdb := infra.Redis(cfg.RedisAddr, cfg.RedisPassword, cfg.RedisDB)
	kw := infra.KafkaWriter(cfg.KafkaBrokers, cfg.KafkaTopic)
	defer infra.CloseKafkaWriter()

	// ── Kafka consumer (background goroutine) ──────────────────────────────
	// Uses a dedicated reader so the producer writer is unaffected.
	reader := infra.NewKafkaReader(cfg.KafkaBrokers, cfg.KafkaTopic, "visionhub-event-consumer")
	consumer.Start(ctx, reader, db)

	// ── Fiber app ──────────────────────────────────────────────────────────
	app := fiber.New(fiber.Config{
		AppName:      "VisionHub Backend v1",
		ReadTimeout:  30 * time.Second,
		WriteTimeout: 30 * time.Second,
		ErrorHandler: jsonErrorHandler,
	})

	app.Use(recover.New())
	app.Use(logger.New(logger.Config{
		Format: "[${time}] ${status} ${latency} ${method} ${path}\n",
	}))

	app.Get("/healthz", func(c *fiber.Ctx) error {
		return c.JSON(fiber.Map{"status": "ok"})
	})

	route.Register(app, db, rdb, kw)

	// ── Graceful shutdown ──────────────────────────────────────────────────
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		<-quit
		log.Println("[main] shutdown signal received")
		cancel() // stops the Kafka consumer goroutine
		if err := app.Shutdown(); err != nil {
			log.Printf("[main] fiber shutdown error: %v", err)
		}
	}()

	log.Printf("[main] starting on :%s", cfg.AppPort)
	if err := app.Listen(":" + cfg.AppPort); err != nil {
		log.Fatalf("[main] listen error: %v", err)
	}
}

// jsonErrorHandler returns all Fiber errors as JSON instead of plain text.
func jsonErrorHandler(c *fiber.Ctx, err error) error {
	code := fiber.StatusInternalServerError
	if e, ok := err.(*fiber.Error); ok {
		code = e.Code
	}
	return c.Status(code).JSON(fiber.Map{
		"success": false,
		"error":   err.Error(),
	})
}
