package service

import (
	"context"
	"crypto/md5"
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/redis/go-redis/v9"
)

const (
	medicineCacheTTL = 30 * 24 * time.Hour
	cacheKeyPrefix   = "medicine:ocr:"
)

// MedicineResult is the value returned to the HTTP handler.
type MedicineResult struct {
	TTSText string `json:"tts_text"`
	Cached  bool   `json:"cached"`
}

// MedicineService holds the dependencies needed to fulfil a recognition request.
type MedicineService struct {
	rdb       *redis.Client
	publisher *EventPublisher // nil-safe: omit in tests that don't need Kafka
}

func NewMedicineService(rdb *redis.Client, publisher *EventPublisher) *MedicineService {
	return &MedicineService{rdb: rdb, publisher: publisher}
}

// Recognize is the core pipeline:
//  1. Simulated OCR  → raw text
//  2. MD5(text)      → Redis key
//  3. Redis GET      → cache hit → return immediately
//  4. Cache miss     → simulated LLM  → Redis SET (30 d TTL)
//  5. Return advice
func (s *MedicineService) Recognize(ctx context.Context, deviceID, imageBase64 string) (*MedicineResult, error) {
	if deviceID == "" {
		return nil, errors.New("device_id is required")
	}
	if imageBase64 == "" {
		return nil, errors.New("image_base64 is required")
	}

	// Step 1: OCR
	ocrText, err := simulateOCR(imageBase64)
	if err != nil {
		return nil, fmt.Errorf("ocr: %w", err)
	}

	// Step 2: cache key
	cacheKey := ocrMD5Key(ocrText)

	// Step 3: Redis lookup
	if cached, err := s.rdb.Get(ctx, cacheKey).Result(); err == nil {
		log.Printf("[medicine] cache hit key=%s device=%s", cacheKey, deviceID)
		if s.publisher != nil {
			s.publisher.PublishMedicine(ctx, deviceID, ocrText, cached, true)
		}
		return &MedicineResult{TTSText: cached, Cached: true}, nil
	} else if !errors.Is(err, redis.Nil) {
		// Redis is degraded — log and fall through to LLM rather than failing the request.
		log.Printf("[medicine] redis GET error (falling through to LLM): %v", err)
	}

	// Step 4: LLM
	advice, err := simulateLLM(ocrText)
	if err != nil {
		return nil, fmt.Errorf("llm: %w", err)
	}

	// Step 5: populate cache (best-effort — don't fail the request on SET error)
	if err := s.rdb.Set(ctx, cacheKey, advice, medicineCacheTTL).Err(); err != nil {
		log.Printf("[medicine] redis SET error: %v", err)
	}

	result := &MedicineResult{TTSText: advice, Cached: false}

	// Async Kafka audit log — best-effort, must not block or fail the HTTP response.
	if s.publisher != nil {
		s.publisher.PublishMedicine(ctx, deviceID, ocrText, advice, false)
	}

	return result, nil
}

// ocrMD5Key builds a deterministic Redis key from OCR text.
// Two images that produce the same text share one cache entry — this is intentional:
// the same drug label should always yield the same advice.
func ocrMD5Key(text string) string {
	sum := md5.Sum([]byte(text))
	return cacheKeyPrefix + fmt.Sprintf("%x", sum)
}

// simulateOCR stands in for a real OCR call (e.g. Tencent Cloud OCR / Aliyun OCR).
// Replace this function body with an actual HTTP call in production.
func simulateOCR(_ string) (string, error) {
	return "阿莫西林胶囊 0.5g 每次2粒 每日3次 饭后服用 有效期2026-01", nil
}

// simulateLLM stands in for a real LLM call (e.g. Qwen / Claude / GPT-4o).
// Replace this function body with a structured prompt + HTTP call in production.
func simulateLLM(ocrText string) (string, error) {
	return fmt.Sprintf(
		"识别到药品信息：%s。用药建议：请严格按说明书或医嘱服用。"+
			"如您对青霉素类抗生素过敏，请立即停药并就医。服药期间避免饮酒。",
		ocrText,
	), nil
}
