package infra

import (
	"log"
	"strings"
	"sync"
	"time"

	"github.com/segmentio/kafka-go"
)

var (
	writerOnce sync.Once
	writer     *kafka.Writer
)

// KafkaWriter returns the singleton kafka.Writer (producer), initializing it on the first call.
// brokers is a comma-separated list, e.g. "host1:9092,host2:9092".
func KafkaWriter(brokers, topic string) *kafka.Writer {
	writerOnce.Do(func() {
		writer = newKafkaWriter(brokers, topic)
	})
	return writer
}

func newKafkaWriter(brokers, topic string) *kafka.Writer {
	addrs := strings.Split(brokers, ",")

	w := &kafka.Writer{
		Addr:         kafka.TCP(addrs...),
		Topic:        topic,
		Balancer:     &kafka.LeastBytes{},
		RequiredAcks: kafka.RequireOne, // at-least-once delivery
		MaxAttempts:  5,
		// Batch settings — tune for throughput vs. latency.
		BatchSize:    100,
		BatchTimeout: 10 * time.Millisecond,
		// Async error logger.
		ErrorLogger: kafka.LoggerFunc(func(msg string, args ...any) {
			log.Printf("[kafka-writer] "+msg, args...)
		}),
	}

	log.Printf("[kafka] writer ready (brokers=%s topic=%s)", brokers, topic)
	return w
}

// CloseKafkaWriter flushes pending messages and closes the writer.
// Call this in a defer or shutdown hook.
func CloseKafkaWriter() {
	if writer != nil {
		if err := writer.Close(); err != nil {
			log.Printf("[kafka] writer close error: %v", err)
		}
	}
}

// NewKafkaReader creates a new kafka.Reader (consumer) for the given group.
// Unlike the writer, readers are not singletons — each consumer group needs its own.
// groupID ensures offset tracking is isolated per logical consumer.
func NewKafkaReader(brokers, topic, groupID string) *kafka.Reader {
	addrs := strings.Split(brokers, ",")

	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:        addrs,
		Topic:          topic,
		GroupID:        groupID,
		MinBytes:       1,        // return as soon as any data is available
		MaxBytes:       10 << 20, // 10 MB max per fetch
		CommitInterval: 0,        // synchronous commit after each ReadMessage
		ErrorLogger: kafka.LoggerFunc(func(msg string, args ...any) {
			log.Printf("[kafka-reader] "+msg, args...)
		}),
	})

	log.Printf("[kafka] reader ready (brokers=%s topic=%s group=%s)", brokers, topic, groupID)
	return r
}
