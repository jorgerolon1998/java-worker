# 🚀 Installation Guide - Order Processing System

## 📋 Prerequisites

Before installing the Order Processing System, ensure you have the following software installed:

### Required Software
- **Docker & Docker Compose**: For running infrastructure services
- **Java 21**: For the Java Worker application
- **Gradle 8.0+**: For building the Java application (optional, includes wrapper)
- **Go 1.21+**: For the Go API services
- **Python 3.8+**: For test scripts (optional)

### System Requirements
- **RAM**: Minimum 4GB, Recommended 8GB
- **Disk Space**: At least 2GB free space
- **CPU**: 2+ cores recommended

## 🔧 Installation Steps

### 1. Clone the Repository
```bash
git clone <repository-url>
cd order-processing-system
```

### 2. Run the Setup Script
```bash
chmod +x scripts/setup.sh
./scripts/setup.sh
```

This script will:
- Check all prerequisites
- Start Docker services (Kafka, MongoDB, Redis)
- Wait for services to be ready
- Display service status

### 3. Install Go Dependencies
```bash
# Product Service
cd go-apis/product-service
go mod tidy

# Customer Service
cd ../customer-service
go mod tidy
```

### 4. Build Java Application
```bash
cd java-worker
./gradlew clean build
```

## 🚀 Running the System

### Option 1: Manual Startup

#### 1. Start Infrastructure Services
```bash
docker-compose up -d
```

#### 2. Start Go APIs
```bash
# Terminal 1 - Product Service
cd go-apis/product-service
go run main.go

# Terminal 2 - Customer Service
cd go-apis/customer-service
go run main.go
```

#### 3. Start Java Worker
```bash
# Terminal 3 - Java Worker
cd java-worker
./gradlew bootRun
```

### Option 2: Using Scripts

#### 1. Start All Services
```bash
# Start infrastructure
docker-compose up -d

# Start Go APIs (in background)
cd go-apis/product-service && go run main.go &
cd ../customer-service && go run main.go &

# Start Java Worker
cd java-worker && ./gradlew bootRun
```

## 🧪 Testing the System

### 1. Send Test Messages
```bash
# Install Python dependencies (if needed)
pip install kafka-python

# Send test messages
python scripts/send-test-messages.py --count 5 --delay 1
```

### 2. Verify System Health
```bash
# Check all services are running
docker-compose ps

# Test health endpoints
curl http://localhost:8081/health  # Product API
curl http://localhost:8082/health  # Customer API
curl http://localhost:8080/actuator/health  # Java Worker
```

### 3. Monitor Kafka Messages
- Open Kafka UI: http://localhost:8080
- Navigate to the "orders" topic
- Check message consumption

## 📊 Monitoring and Debugging

### Service URLs
- **Kafka UI**: http://localhost:8080
- **Product API**: http://localhost:8081
- **Customer API**: http://localhost:8082
- **Java Worker**: http://localhost:8080

### Health Checks
```bash
# Product API Health
curl http://localhost:8081/health

# Customer API Health
curl http://localhost:8082/health

# Java Worker Health
curl http://localhost:8080/actuator/health

# Java Worker Metrics
curl http://localhost:8080/actuator/metrics
```

### Logs
```bash
# Docker services logs
docker-compose logs kafka
docker-compose logs mongodb
docker-compose logs redis

# Java Worker logs
cd java-worker
mvn spring-boot:run 2>&1 | tee logs/java-worker.log

# Go API logs (visible in terminal)
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Docker Services Not Starting
```bash
# Check Docker status
docker --version
docker-compose --version

# Restart Docker services
docker-compose down
docker-compose up -d

# Check service logs
docker-compose logs
```

#### 2. Java Application Won't Start
```bash
# Check Java version
java -version

# Check Gradle
./gradlew --version

# Clean and rebuild
cd java-worker
./gradlew clean build
```

#### 3. Go APIs Won't Start
```bash
# Check Go version
go version

# Install dependencies
cd go-apis/product-service
go mod tidy
go mod download

cd ../customer-service
go mod tidy
go mod download
```

#### 4. Kafka Connection Issues
```bash
# Check if Kafka is running
docker-compose ps kafka

# Test Kafka connectivity
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

#### 5. MongoDB Connection Issues
```bash
# Check MongoDB status
docker-compose ps mongodb

# Test MongoDB connection
docker exec -it mongodb mongosh --eval "db.adminCommand('ping')"
```

#### 6. Redis Connection Issues
```bash
# Check Redis status
docker-compose ps redis

# Test Redis connection
docker exec -it redis redis-cli ping
```

### Debug Commands

#### Check Service Status
```bash
# All services
docker-compose ps

# Specific service
docker-compose ps kafka
```

#### View Logs
```bash
# All logs
docker-compose logs

# Specific service logs
docker-compose logs kafka
docker-compose logs mongodb
docker-compose logs redis
```

#### Test Connectivity
```bash
# Test Kafka
kafka-console-producer --bootstrap-server localhost:9092 --topic test

# Test MongoDB
mongo mongodb://localhost:27017/orders

# Test Redis
redis-cli ping
```

## 🧹 Cleanup

### Stop All Services
```bash
# Stop Docker services
docker-compose down

# Stop Go APIs (if running in background)
pkill -f "go run main.go"

# Stop Java Worker (Ctrl+C in terminal)
```

### Remove Data (Optional)
```bash
# Remove Docker volumes (WARNING: This will delete all data)
docker-compose down -v

# Remove Docker images
docker-compose down --rmi all
```

## 📈 Performance Tuning

### Java Worker
```bash
# Increase heap size
export JAVA_OPTS="-Xmx2g -Xms1g"

# Run with specific profile
mvn spring-boot:run -Dspring.profiles.active=prod
```

### Go APIs
```bash
# Set environment variables for performance
export GOMAXPROCS=4
export GOGC=100

# Run with specific configuration
PORT=8081 go run main.go
```

### Docker Resources
```bash
# Increase Docker memory limit
# Edit docker-compose.yml and add:
# deploy:
#   resources:
#     limits:
#       memory: 2G
```

## 🔒 Security Considerations

### Development Environment
- All services run on localhost
- No external network access required
- Default credentials for development only

### Production Deployment
- Use proper secrets management
- Enable SSL/TLS for all communications
- Implement proper authentication and authorization
- Use production-grade databases and message queues

## 📚 Additional Resources

### Documentation
- [Technical Design](docs/TECHNICAL_DESIGN.md)
- [API Documentation](README.md#api-documentation)
- [Troubleshooting Guide](docs/TECHNICAL_DESIGN.md#troubleshooting)

### Useful Commands
```bash
# Quick health check
curl -s http://localhost:8081/health && echo "Product API: OK" || echo "Product API: FAILED"
curl -s http://localhost:8082/health && echo "Customer API: OK" || echo "Customer API: FAILED"
curl -s http://localhost:8080/actuator/health && echo "Java Worker: OK" || echo "Java Worker: FAILED"

# Check system resources
docker stats

# Monitor logs in real-time
docker-compose logs -f
```

## 🆘 Getting Help

If you encounter issues:

1. **Check the logs**: Use `docker-compose logs` to see service logs
2. **Verify prerequisites**: Ensure all required software is installed
3. **Check connectivity**: Test if all services can communicate
4. **Review configuration**: Verify environment variables and settings
5. **Check resources**: Ensure sufficient memory and disk space

For additional support, please refer to the troubleshooting section in the technical documentation. 