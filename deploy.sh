#!/bin/bash
set -e

echo "=== VisionHub Backend Deployment ==="

# Check Docker
echo "Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "Error: Docker not found"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "Error: Docker Compose not found"
    exit 1
fi

# Create .env if not exists
if [ ! -f backend/.env ]; then
    echo "Creating backend/.env from example..."
    cp backend/.env.example backend/.env
    echo "Warning: Please edit backend/.env with your actual configuration"
fi

# Start services
echo "Starting services..."
docker-compose -f backend/docker-compose.deploy.yml --env-file backend/.env up -d --build

echo ""
echo "Waiting for services to start..."
sleep 5

# Health check
echo "Checking health..."
for i in {1..10}; do
    if curl -s http://localhost:3000/healthz | grep -q '"status":"ok"'; then
        echo "✓ Backend is healthy"
        break
    fi
    echo "  Attempt $i/10..."
    sleep 2
done

echo ""
echo "=== Deployment Complete ==="
echo "API: http://localhost:3000"
echo "Health: http://localhost:3000/healthz"
