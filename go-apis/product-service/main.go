package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
	"github.com/google/uuid"
	"github.com/sirupsen/logrus"
)

// Product represents a product entity
type Product struct {
	ID          string  `json:"id"`
	Name        string  `json:"name"`
	Description string  `json:"description"`
	Price       float64 `json:"price"`
	Active      bool    `json:"active"`
	CreatedAt   string  `json:"createdAt"`
	UpdatedAt   string  `json:"updatedAt"`
}

// ProductService handles product operations
type ProductService struct {
	products map[string]*Product
	redis    *redis.Client
	logger   *logrus.Logger
}

// NewProductService creates a new product service
func NewProductService(redisClient *redis.Client) *ProductService {
	service := &ProductService{
		products: make(map[string]*Product),
		redis:    redisClient,
		logger:   logrus.New(),
	}

	// Initialize with sample data
	service.initializeSampleData()
	return service
}

// initializeSampleData populates the service with sample products
func (s *ProductService) initializeSampleData() {
	products := []*Product{
		{
			ID:          "product-001",
			Name:        "Laptop Gaming",
			Description: "High-performance gaming laptop with RTX 4080",
			Price:       2499.99,
			Active:      true,
			CreatedAt:   time.Now().Format(time.RFC3339),
			UpdatedAt:   time.Now().Format(time.RFC3339),
		},
		{
			ID:          "product-002",
			Name:        "Smartphone Pro",
			Description: "Latest smartphone with advanced camera system",
			Price:       999.99,
			Active:      true,
			CreatedAt:   time.Now().Format(time.RFC3339),
			UpdatedAt:   time.Now().Format(time.RFC3339),
		},
		{
			ID:          "product-003",
			Name:        "Wireless Headphones",
			Description: "Noise-cancelling wireless headphones",
			Price:       299.99,
			Active:      true,
			CreatedAt:   time.Now().Format(time.RFC3339),
			UpdatedAt:   time.Now().Format(time.RFC3339),
		},
		{
			ID:          "product-004",
			Name:        "Gaming Mouse",
			Description: "High-precision gaming mouse with RGB",
			Price:       89.99,
			Active:      true,
			CreatedAt:   time.Now().Format(time.RFC3339),
			UpdatedAt:   time.Now().Format(time.RFC3339),
		},
		{
			ID:          "product-005",
			Name:        "Mechanical Keyboard",
			Description: "Cherry MX Blue mechanical keyboard",
			Price:       149.99,
			Active:      true,
			CreatedAt:   time.Now().Format(time.RFC3339),
			UpdatedAt:   time.Now().Format(time.RFC3339),
		},
	}

	for _, product := range products {
		s.products[product.ID] = product
	}
}

// GetProduct retrieves a product by ID with caching
func (s *ProductService) GetProduct(ctx context.Context, productID string) (*Product, error) {
	// Try to get from cache first
	cacheKey := fmt.Sprintf("product:%s", productID)
	cachedProduct, err := s.redis.Get(ctx, cacheKey).Result()
	if err == nil {
		var product Product
		if err := json.Unmarshal([]byte(cachedProduct), &product); err == nil {
			s.logger.Infof("Product %s retrieved from cache", productID)
			return &product, nil
		}
	}

	// Get from memory
	product, exists := s.products[productID]
	if !exists {
		return nil, fmt.Errorf("product not found: %s", productID)
	}

	// Cache the product
	if productJSON, err := json.Marshal(product); err == nil {
		s.redis.Set(ctx, cacheKey, productJSON, 1*time.Hour)
	}

	s.logger.Infof("Product %s retrieved from memory", productID)
	return product, nil
}

// GetAllProducts returns all products
func (s *ProductService) GetAllProducts() []*Product {
	products := make([]*Product, 0, len(s.products))
	for _, product := range s.products {
		products = append(products, product)
	}
	return products
}

// CreateProduct creates a new product
func (s *ProductService) CreateProduct(product *Product) error {
	if product.ID == "" {
		product.ID = fmt.Sprintf("product-%s", uuid.New().String()[:8])
	}
	product.CreatedAt = time.Now().Format(time.RFC3339)
	product.UpdatedAt = time.Now().Format(time.RFC3339)
	s.products[product.ID] = product
	return nil
}

// UpdateProduct updates an existing product
func (s *ProductService) UpdateProduct(productID string, product *Product) error {
	if _, exists := s.products[productID]; !exists {
		return fmt.Errorf("product not found: %s", productID)
	}
	product.ID = productID
	product.UpdatedAt = time.Now().Format(time.RFC3339)
	s.products[productID] = product
	return nil
}

// DeleteProduct deletes a product
func (s *ProductService) DeleteProduct(productID string) error {
	if _, exists := s.products[productID]; !exists {
		return fmt.Errorf("product not found: %s", productID)
	}
	delete(s.products, productID)
	return nil
}

// setupRoutes configures the HTTP routes
func setupRoutes(productService *ProductService) *gin.Engine {
	r := gin.Default()

	// Health check
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":  "healthy",
			"service": "product-service",
			"time":    time.Now().Format(time.RFC3339),
		})
	})

	// API routes
	api := r.Group("/api")
	{
		products := api.Group("/products")
		{
			products.GET("", func(c *gin.Context) {
				products := productService.GetAllProducts()
				c.JSON(http.StatusOK, products)
			})

			products.GET("/:id", func(c *gin.Context) {
				productID := c.Param("id")
				product, err := productService.GetProduct(c.Request.Context(), productID)
				if err != nil {
					c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
					return
				}
				c.JSON(http.StatusOK, product)
			})

			products.POST("", func(c *gin.Context) {
				var product Product
				if err := c.ShouldBindJSON(&product); err != nil {
					c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
					return
				}

				if err := productService.CreateProduct(&product); err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
					return
				}

				c.JSON(http.StatusCreated, product)
			})

			products.PUT("/:id", func(c *gin.Context) {
				productID := c.Param("id")
				var product Product
				if err := c.ShouldBindJSON(&product); err != nil {
					c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
					return
				}

				if err := productService.UpdateProduct(productID, &product); err != nil {
					c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
					return
				}

				c.JSON(http.StatusOK, product)
			})

			products.DELETE("/:id", func(c *gin.Context) {
				productID := c.Param("id")
				if err := productService.DeleteProduct(productID); err != nil {
					c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
					return
				}

				c.JSON(http.StatusOK, gin.H{"message": "Product deleted successfully"})
			})
		}
	}

	return r
}

func main() {
	// Get port from environment or use default
	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	// Initialize Redis client
	redisAddr := os.Getenv("REDIS_ADDR")
	if redisAddr == "" {
		redisAddr = "localhost:6379"
	}

	redisClient := redis.NewClient(&redis.Options{
		Addr:     redisAddr,
		Password: "",
		DB:       0,
	})

	// Test Redis connection
	ctx := context.Background()
	if err := redisClient.Ping(ctx).Err(); err != nil {
		log.Printf("Warning: Redis connection failed: %v", err)
	} else {
		log.Println("Redis connection established")
	}

	// Initialize product service
	productService := NewProductService(redisClient)

	// Setup routes
	r := setupRoutes(productService)

	// Start server
	log.Printf("Product service starting on port %s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
