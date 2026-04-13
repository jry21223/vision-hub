package route

import (
	"github.com/example/visionhub-backend/handler"
	"github.com/example/visionhub-backend/service"
	"github.com/gofiber/fiber/v2"
	"github.com/redis/go-redis/v9"
	"github.com/segmentio/kafka-go"
	"gorm.io/gorm"
)

// Register wires all API routes onto the given Fiber app.
// All dependencies are injected here; no package-level globals.
func Register(app *fiber.App, _ *gorm.DB, rdb *redis.Client, kw *kafka.Writer) {
	publisher := service.NewEventPublisher(kw)

	v1 := app.Group("/api/v1")

	// ── Medicine recognition ───────────────────────────────────────────────
	medicineSvc := service.NewMedicineService(rdb, publisher)
	v1.Post("/recognize/medicine", handler.NewMedicineHandler(medicineSvc).Recognize)

	// ── Fall / event reporting ─────────────────────────────────────────────
	v1.Post("/event/report", handler.NewEventHandler(publisher).ReportFall)
}
