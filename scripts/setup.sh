#!/bin/bash

# Setup script for Order Processing System
echo "🚀 Setting up Order Processing System..."

# Create necessary directories
echo "📁 Creating directories..."
mkdir -p logs
mkdir -p data

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Check if Java 21 is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 first."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "21" ]; then
    echo "❌ Java 21 is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Check if Gradle is installed
if ! command -v gradle &> /dev/null; then
    echo "⚠️  Gradle is not installed. Will use Gradle wrapper if available."
fi

# Check if Go is installed
if ! command -v go &> /dev/null; then
    echo "❌ Go is not installed. Please install Go 1.21+ first."
    exit 1
fi

echo "✅ Prerequisites check passed!"

# Start Docker services
echo "🐳 Starting Docker services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check if services are running
echo "🔍 Checking service status..."
docker-compose ps

echo "✅ Setup completed successfully!"
echo ""
echo "📋 Next steps:"
echo "1. Start the Go APIs:"
echo "   cd go-apis/product-service && go mod tidy && go run main.go"
echo "   cd go-apis/customer-service && go mod tidy && go run main.go"
echo ""
echo "2. Start the Java Worker:"
echo "   cd java-worker && ./gradlew bootRun"
echo ""
echo "3. Access services:"
echo "   - Kafka UI: http://localhost:8080"
echo "   - Product API: http://localhost:8081"
echo "   - Customer API: http://localhost:8082"
echo "   - Java Worker: http://localhost:8080" 