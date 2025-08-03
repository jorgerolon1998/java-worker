# 🏗️ Technical Design Document - Order Processing System

## 📋 Overview

This document describes the technical architecture and implementation details of the Order Processing System, which consists of a Java Worker that processes order messages from Kafka, enriches data by calling Go APIs, and stores results in MongoDB.

## 🏛️ Architecture Overview

### System Components

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Kafka     │    │   Redis     │    │  MongoDB    │
│  (Messages) │    │ (Cache/Lock)│    │ (Storage)   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────┐
│              Java Worker (Spring Boot)                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │
│  │   Consumer  │  │  Processor  │  │   Storage   │   │
│  └─────────────┘  └─────────────┘  └─────────────┘   │
└─────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────┐    ┌─────────────┐
│ Product API │    │Customer API │
│   (Go)      │    │   (Go)      │
└─────────────┘    └─────────────┘
```

### Clean Architecture Implementation

The Java Worker follows Clean Architecture principles:

```
📁 java-worker/
├── 📁 domain/           # Business entities and rules
│   ├── entities/        # Order, Product, Customer entities
│   ├── repositories/    # Repository interfaces
│   └── services/        # Domain services
├── 📁 application/      # Use cases and application logic
│   ├── usecases/        # ProcessOrderUseCase
│   └── dtos/           # Data Transfer Objects
├── 📁 infrastructure/   # External adapters
│   ├── kafka/          # Kafka consumer
│   ├── mongodb/        # MongoDB repository
│   ├── redis/          # Cache and distributed locks
│   └── external/       # Go API clients
└── 📁 interfaces/      # Controllers and configuration
    ├── controllers/     # REST controllers
    └── config/         # Spring configuration
```

## 🔄 Data Flow

### 1. Message Consumption
- Kafka consumer receives order messages
- Messages contain: `orderId`, `customerId`, `productIds[]`
- Manual acknowledgment for control

### 2. Distributed Lock
- Redis-based distributed lock prevents duplicate processing
- Lock TTL: 30 seconds
- Lock key: `order:lock:{orderId}`

### 3. Data Enrichment
- **Product Service**: Fetches product details (name, price, description)
- **Customer Service**: Fetches customer details (name, email, credit limit)
- Circuit breakers and retry logic for resilience

### 4. Business Validation
- Customer status validation (must be active)
- Product availability validation (must be active)
- Credit limit validation

### 5. Data Persistence
- MongoDB storage with proper indexing
- Order status tracking
- Audit trail with timestamps

## 🛡️ Resilience Patterns

### Circuit Breaker
```yaml
resilience4j:
  circuitbreaker:
    instances:
      productService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
```

### Retry Logic
```yaml
resilience4j:
  retry:
    instances:
      productService:
        max-attempts: 3
        wait-duration: 1s
        enable-exponential-backoff: true
```

### Error Handling
- Failed messages stored in Redis with retry count
- Dead letter queue for permanently failed messages
- Comprehensive logging and monitoring

## 💾 Caching Strategy

### Redis Cache Configuration
- **Products**: TTL 1 hour (stable data)
- **Customers**: TTL 30 minutes (can change status)
- **Cache keys**: `product:{productId}`, `customer:{customerId}`

### Cache Invalidation
- Automatic TTL-based expiration
- Manual invalidation on updates
- Cache-aside pattern

## 🔒 Security Considerations

### Input Validation
- Bean validation annotations
- JSON schema validation
- SQL injection prevention (MongoDB)

### Rate Limiting
- Kafka consumer throttling
- API rate limiting in Go services
- Redis-based rate limiting

### Secrets Management
- Environment variables for sensitive data
- No hardcoded credentials
- Docker secrets for production

## 📊 Monitoring and Observability

### Health Checks
- Spring Boot Actuator endpoints
- `/actuator/health` - Overall health
- `/actuator/metrics` - Performance metrics

### Metrics
- Kafka consumer lag
- Processing time per order
- Error rates and circuit breaker status
- Cache hit/miss ratios

### Logging
- Structured logging with correlation IDs
- Log levels: INFO, WARN, ERROR
- Centralized log aggregation

## 🚀 Performance Optimization

### Connection Pooling
- MongoDB connection pool: 10-50 connections
- Redis connection pool: 8 connections
- Kafka consumer threads: 3 concurrent

### Indexing Strategy
```javascript
// MongoDB indexes
db.orders.createIndex({ "orderId": 1 }, { unique: true });
db.orders.createIndex({ "customerId": 1 });
db.orders.createIndex({ "createdAt": 1 });
db.orders.createIndex({ "status": 1 });
```

### Caching Benefits
- Reduced API calls to Go services
- Faster response times
- Lower external service load

## 🔧 Configuration Management

### Environment Variables
```bash
# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_TOPIC_ORDERS=orders

# MongoDB
MONGODB_URI=mongodb://localhost:27017/orders

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# APIs
PRODUCT_API_URL=http://localhost:8081
CUSTOMER_API_URL=http://localhost:8082
```

### Profiles
- **dev**: Development configuration
- **test**: Testing configuration
- **prod**: Production configuration

## 🧪 Testing Strategy

### Unit Tests
- Domain logic testing
- Repository layer testing
- Service layer testing

### Integration Tests
- Kafka consumer testing
- MongoDB integration testing
- Redis integration testing

### Contract Tests
- Go API contract testing
- Message format validation

### Performance Tests
- Load testing with multiple orders
- Stress testing with high message volume
- End-to-end latency testing

## 📈 Scalability Considerations

### Horizontal Scaling
- Multiple Java Worker instances
- Kafka partitioning for parallel processing
- Load balancing for Go APIs

### Vertical Scaling
- JVM heap optimization
- Connection pool tuning
- Cache size optimization

### Database Scaling
- MongoDB replica sets
- Redis clustering
- Kafka cluster configuration

## 🔄 Deployment Strategy

### Docker Compose
- All services containerized
- Service discovery via Docker network
- Volume persistence for data

### Production Considerations
- Kubernetes deployment
- Service mesh (Istio)
- Monitoring stack (Prometheus + Grafana)
- Log aggregation (ELK stack)

## 🛠️ Development Workflow

### Local Development
1. Start infrastructure services: `docker-compose up -d`
2. Start Go APIs: `go run main.go`
3. Start Java Worker: `mvn spring-boot:run`
4. Send test messages: `python scripts/send-test-messages.py`

### Testing
1. Unit tests: `mvn test`
2. Integration tests: `mvn verify`
3. End-to-end tests: Manual testing with test messages

### Monitoring
1. Kafka UI: http://localhost:8080
2. Health checks: `/actuator/health`
3. Metrics: `/actuator/metrics`

## 📚 API Documentation

### Go APIs

#### Product Service (Port 8081)
```
GET /api/products/{id}     # Get product details
GET /api/products          # Get all products
POST /api/products         # Create product
PUT /api/products/{id}     # Update product
DELETE /api/products/{id}  # Delete product
```

#### Customer Service (Port 8082)
```
GET /api/customers/{id}     # Get customer details
GET /api/customers          # Get all customers
POST /api/customers         # Create customer
PUT /api/customers/{id}     # Update customer
DELETE /api/customers/{id}  # Delete customer
```

### Java Worker (Port 8080)
```
GET /actuator/health        # Health check
GET /actuator/metrics       # Metrics
GET /actuator/info          # Application info
```

## 🔍 Troubleshooting

### Common Issues

1. **Kafka Connection Issues**
   - Check if Kafka is running: `docker-compose ps`
   - Verify bootstrap servers configuration

2. **MongoDB Connection Issues**
   - Check MongoDB container status
   - Verify connection string and credentials

3. **Redis Connection Issues**
   - Check Redis container status
   - Verify host and port configuration

4. **Go API Issues**
   - Check if Go services are running
   - Verify API endpoints are accessible
   - Check logs for errors

### Debug Commands
```bash
# Check service status
docker-compose ps

# View logs
docker-compose logs kafka
docker-compose logs mongodb
docker-compose logs redis

# Test Kafka connectivity
kafka-console-producer --bootstrap-server localhost:9092 --topic orders

# Test MongoDB connectivity
mongo mongodb://localhost:27017/orders

# Test Redis connectivity
redis-cli ping
```

## 📝 Future Enhancements

### Planned Features
1. **Event Sourcing**: Complete audit trail of order changes
2. **CQRS**: Separate read and write models
3. **Saga Pattern**: Distributed transaction management
4. **API Gateway**: Centralized API management
5. **Service Mesh**: Advanced service communication

### Performance Improvements
1. **Async Processing**: Non-blocking order processing
2. **Batch Processing**: Process multiple orders together
3. **Stream Processing**: Real-time analytics
4. **Caching Optimization**: Multi-level caching strategy

### Monitoring Enhancements
1. **Distributed Tracing**: Jaeger integration
2. **Advanced Metrics**: Custom business metrics
3. **Alerting**: Proactive issue detection
4. **Dashboard**: Real-time system overview 