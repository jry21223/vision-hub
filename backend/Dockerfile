FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY go.mod ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o visionhub-backend ./main.go

FROM alpine:3.20
RUN apk --no-cache add ca-certificates tzdata
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=builder /app/visionhub-backend ./visionhub-backend
EXPOSE 3000
CMD ["./visionhub-backend"]
