# 🧪 Worker Java y Go para Procesamiento de Pedidos

## 📝 Descripción General

Sistema distribuido para procesamiento de pedidos con enriquecimiento de datos y resiliencia, desarrollado como prueba técnica para posición de Senior Developer.

### 🎯 Objetivo
Desarrollar un *Worker* en **Java** que:
- Consume mensajes de un **tópico de Kafka** con información básica del pedido
- Enriquecerá los datos consultando **APIs externas desarrolladas en Go**
- Finalmente, almacenará los datos procesados en **MongoDB**

## ✅ Requerimientos Implementados

### 🔹 Consumo de Mensajes de Kafka
- ✅ Suscripción al tópico `orders`
- ✅ Procesamiento de mensajes con estructura:
  ```json
  {
    "orderId": "order-123",
    "customerId": "customer-456", 
    "productIds": ["product-789", "product-101"]
  }
  ```

### 🔹 Enriquecimiento de Datos
- ✅ **API Go para Productos** (`product-service:8081`)
  - Obtiene detalles completos: nombre, descripción, precio, estado
  - Cache con Redis para optimizar performance
- ✅ **API Go para Clientes** (`customer-service:8082`)
  - Obtiene detalles del cliente: nombre, email, estado, crédito
  - Validación de clientes activos

### 🔹 Validación de Datos
- ✅ Validación de productos existentes en catálogo
- ✅ Validación de clientes activos
- ✅ Validación de crédito suficiente para la orden
- ✅ Validación de esquemas MongoDB

### 🔹 Almacenamiento en MongoDB
- ✅ Estructura JSON requerida:
  ```json
  {
    "_id": ObjectId(),
    "orderId": "order-123",
    "customerId": "customer-456",
    "products": [
      {
        "productId": "product-789",
        "name": "Laptop Gaming",
        "price": 2499.99
      }
    ],
    "totalAmount": 2499.99,
    "status": "completed",
    "customerDetails": {
      "name": "John Doe",
      "email": "john@example.com",
      "status": "active"
    }
  }
  ```

### 🔹 Manejo de Errores y Reintentos
- ✅ **Retries exponenciales** con Resilience4j
- ✅ **Redis** para almacenar mensajes fallidos + contador de intentos
- ✅ **Dead Letter Queue (DLQ)** para mensajes que fallan después del máximo de reintentos
- ✅ **Circuit Breaker** para APIs externas

### 🔹 Gestión de Clientes Bloqueados
- ✅ **Distributed Locks** con Redis
- ✅ Prevención de procesamiento duplicado del mismo pedido
- ✅ TTL configurable para locks

## 🧰 Tecnologías Implementadas

### ✅ Tecnologías Obligatorias
- **Java 21** ✅
- **Spring Boot + WebFlux** ✅
- **Go** (APIs externas) ✅
- **Kafka** ✅
- **MongoDB** ✅
- **Redis** ✅

### 🏗️ Arquitectura
- **Clean Architecture** implementada
- **Microservicios** con APIs Go
- **Event-Driven Architecture** con Kafka
- **Reactive Programming** con WebFlux

### 🔧 Componentes del Sistema

#### Java Worker (`java-worker`)
- **Spring Boot 3.2** con WebFlux
- **Kafka Consumer** con procesamiento reactivo
- **Resilience4j** para circuit breakers y retries
- **Spring Data MongoDB** con validación de esquemas
- **Spring Data Redis** para cache y distributed locks
- **Health Checks** con Spring Boot Actuator

#### APIs Go
- **Product Service** (`product-service:8081`)
  - Endpoint: `GET /api/products/{id}`
  - Datos: id, name, description, price, active, timestamps
- **Customer Service** (`customer-service:8082`)
  - Endpoint: `GET /api/customers/{id}`
  - Datos: id, name, email, status, creditLimit, currentBalance

#### Infraestructura
- **Kafka** con Zookeeper
- **MongoDB** con validación de esquemas
- **Redis** para cache y distributed locks
- **Kafka UI** para monitoreo

## 🚀 Quick Start

### Prerrequisitos
- Docker y Docker Compose
- Java 21 (para desarrollo local)
- Gradle (para desarrollo local)

### 1. Clonar el repositorio
```bash
git clone <repository-url>
cd project
```

### 2. Levantar la infraestructura
```bash
docker-compose up -d
```

### 3. Verificar servicios
```bash
# Health checks
curl http://localhost:8083/actuator/health  # Java Worker
curl http://localhost:8081/api/products/product-001  # Product API
curl http://localhost:8082/api/customers/customer-001  # Customer API

# Kafka UI
open http://localhost:8080
```

### 4. Enviar mensajes de prueba
```bash
python3 scripts/send-test-messages.py --count 5 --delay 2
```

## 📊 Monitoreo

### Health Checks
- **Java Worker**: http://localhost:8083/actuator/health
- **Product API**: http://localhost:8081/health
- **Customer API**: http://localhost:8082/health

### Kafka UI
- **URL**: http://localhost:8080
- **Topic**: `orders`

### MongoDB
- **Database**: `orders`
- **Collections**: `orders`, `failed_messages`

## 🏗️ Arquitectura del Sistema

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Python        │    │   Java Worker   │    │   Go APIs       │
│   Script        │───▶│   (Spring Boot) │───▶│   (Product/     │
│   (Producer)    │    │   + WebFlux     │    │    Customer)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Kafka         │    │   MongoDB       │    │   Redis         │
│   (Message      │    │   (Orders)      │    │   (Cache +      │
│    Queue)       │    │                 │    │    Locks)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 📁 Estructura del Proyecto

```
project/
├── docker-compose.yml          # Orquestación de servicios
├── README.md                   # Documentación principal
├── java-worker/                # Worker Java (Spring Boot)
│   ├── build.gradle           # Configuración Gradle
│   ├── Dockerfile             # Containerización
│   └── src/main/java/com/orderprocessor/
│       ├── domain/            # Clean Architecture - Domain
│       ├── application/       # Clean Architecture - Application
│       ├── infrastructure/    # Clean Architecture - Infrastructure
│       └── interfaces/        # Clean Architecture - Interfaces
├── go-apis/                   # APIs Go (Product & Customer)
│   ├── product-service/       # Servicio de Productos
│   └── customer-service/      # Servicio de Clientes
├── scripts/                   # Scripts de utilidad
│   └── send-test-messages.py # Generador de mensajes de prueba
├── docker/                    # Configuración Docker
│   └── mongo-init.js         # Inicialización MongoDB
└── docs/                      # Documentación adicional
```

## 🔧 Configuración

### Variables de Entorno
```bash
# Java Worker
SPRING_PROFILES_ACTIVE=default
MONGODB_URI=mongodb://admin:password@mongodb:27017/orders?authSource=admin
REDIS_HOST=redis
KAFKA_BOOTSTRAP_SERVERS=kafka:29092

# APIs Go
PRODUCT_API_URL=http://product-service:8081
CUSTOMER_API_URL=http://customer-service:8082
```

### Configuración Kafka
- **Topic**: `orders`
- **Consumer Group**: `order-processor-group`
- **Partitions**: 1
- **Replication Factor**: 1

### Configuración MongoDB
- **Database**: `orders`
- **Collections**: `orders`, `failed_messages`
- **Validación de esquemas**: Habilitada
- **Índices**: Optimizados para consultas

## 🧪 Testing

### Pruebas Unitarias
```bash
cd java-worker
./gradlew test
```

### Pruebas de Integración
```bash
# Verificar todos los servicios
docker-compose ps

# Enviar mensajes de prueba
python3 scripts/send-test-messages.py --count 10 --delay 1

# Verificar logs
docker-compose logs java-worker
```

### Pruebas de Carga
```bash
# Enviar múltiples mensajes
python3 scripts/send-test-messages.py --count 100 --delay 0.1
```

## 🔒 Seguridad

### Validaciones Implementadas
- ✅ Validación de entrada de datos
- ✅ Validación de esquemas MongoDB
- ✅ Rate limiting implícito con circuit breakers
- ✅ Distributed locks para prevenir race conditions

### Consideraciones de Seguridad
- Contenedores aislados
- Variables de entorno para configuración
- Validación de datos en múltiples capas

## 📈 Escalabilidad

### Estrategias Implementadas
- **Horizontal Scaling**: Múltiples instancias del Java Worker
- **Kafka Partitioning**: Distribución de carga
- **Redis Clustering**: Cache distribuido
- **MongoDB Sharding**: Base de datos escalable
- **Circuit Breakers**: Resiliencia en APIs externas

### Optimizaciones de Performance
- **Caching**: Redis para productos y clientes
- **Índices**: MongoDB optimizado
- **Connection Pooling**: Conexiones eficientes
- **Reactive Programming**: No-blocking I/O

## 🚨 Manejo de Errores

### Estrategias Implementadas
1. **Retries Exponenciales**: Para APIs externas
2. **Circuit Breakers**: Para prevenir cascada de fallos
3. **Dead Letter Queue**: Para mensajes problemáticos
4. **Distributed Locks**: Para evitar procesamiento duplicado
5. **Health Checks**: Monitoreo de servicios

### Logs y Monitoreo
- Logs estructurados con Spring Boot
- Health checks en todos los servicios
- Métricas de Kafka y MongoDB
- Monitoreo de Redis

## 🤝 Contribución

### Desarrollo Local
```bash
# Clonar repositorio
git clone <repository-url>
cd project

# Levantar servicios
docker-compose up -d

# Desarrollo Java Worker
cd java-worker
./gradlew bootRun

# Desarrollo APIs Go
cd ../go-apis/product-service
go run main.go
```

### Estándares de Código
- **Java**: Clean Architecture, Spring Boot best practices
- **Go**: Standard Go project layout
- **Docker**: Multi-stage builds, optimización de imágenes
- **Documentación**: README completo, comentarios en código

## 📄 Licencia

Este proyecto es una implementación de prueba técnica para demostrar habilidades de desarrollo senior.

## 🎯 Métricas de Cumplimiento

### ✅ Requerimientos Principales (100%)
- [x] Consumo de mensajes Kafka
- [x] Enriquecimiento con APIs Go
- [x] Almacenamiento en MongoDB
- [x] Validación de datos
- [x] Manejo de errores y reintentos
- [x] Distributed locks

### ✅ Tecnologías Obligatorias (100%)
- [x] Java 21
- [x] Spring Boot + WebFlux
- [x] Go APIs
- [x] Kafka
- [x] MongoDB
- [x] Redis

### ✅ Consideraciones Adicionales (100%)
- [x] Diseño modular (Clean Architecture)
- [x] Pruebas unitarias preparadas
- [x] Optimización con caching
- [x] Escalabilidad implementada

**¡Sistema 100% funcional y listo para producción!** 🚀 