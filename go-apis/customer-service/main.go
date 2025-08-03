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

// Customer represents a customer entity
type Customer struct {
	ID             string  `json:"id"`
	Name           string  `json:"name"`
	Email          string  `json:"email"`
	Status         string  `json:"status"`
	CreditLimit    float64 `json:"creditLimit"`
	CurrentBalance float64 `json:"currentBalance"`
	CreatedAt      string  `json:"createdAt"`
	UpdatedAt      string  `json:"updatedAt"`
}

// CustomerService handles customer operations
type CustomerService struct {
	customers map[string]*Customer
	redis     *redis.Client
	logger    *logrus.Logger
}

// NewCustomerService creates a new customer service
func NewCustomerService(redisClient *redis.Client) *CustomerService {
	service := &CustomerService{
		customers: make(map[string]*Customer),
		redis:     redisClient,
		logger:    logrus.New(),
	}

	// Initialize with sample data
	service.initializeSampleData()
	return service
}

// initializeSampleData populates the service with sample customers
func (s *CustomerService) initializeSampleData() {
	customers := []*Customer{
		{
			ID:             "customer-001",
			Name:           "John Doe",
			Email:          "john.doe@example.com",
			Status:         "active",
			CreditLimit:    5000.0,
			CurrentBalance: 0.0,
			CreatedAt:      time.Now().Format(time.RFC3339),
			UpdatedAt:      time.Now().Format(time.RFC3339),
		},
		{
			ID:             "customer-002",
			Name:           "Jane Smith",
			Email:          "jane.smith@example.com",
			Status:         "active",
			CreditLimit:    3000.0,
			CurrentBalance: 500.0,
			CreatedAt:      time.Now().Format(time.RFC3339),
			UpdatedAt:      time.Now().Format(time.RFC3339),
		},
		{
			ID:             "customer-003",
			Name:           "Bob Johnson",
			Email:          "bob.johnson@example.com",
			Status:         "active",
			CreditLimit:    10000.0,
			CurrentBalance: 2500.0,
			CreatedAt:      time.Now().Format(time.RFC3339),
			UpdatedAt:      time.Now().Format(time.RFC3339),
		},
		{
			ID:             "customer-004",
			Name:           "Alice Brown",
			Email:          "alice.brown@example.com",
			Status:         "inactive",
			CreditLimit:    2000.0,
			CurrentBalance: 0.0,
			CreatedAt:      time.Now().Format(time.RFC3339),
			UpdatedAt:      time.Now().Format(time.RFC3339),
		},
		{
			ID:             "customer-005",
			Name:           "Charlie Wilson",
			Email:          "charlie.wilson@example.com",
			Status:         "active",
			CreditLimit:    7500.0,
			CurrentBalance: 1000.0,
			CreatedAt:      time.Now().Format(time.RFC3339),
			UpdatedAt:      time.Now().Format(time.RFC3339),
		},
	}

	for _, customer := range customers {
		s.customers[customer.ID] = customer
	}
}

// GetCustomer retrieves a customer by ID with caching
func (s *CustomerService) GetCustomer(ctx context.Context, customerID string) (*Customer, error) {
	// Try to get from cache first
	cacheKey := fmt.Sprintf("customer:%s", customerID)
	cachedCustomer, err := s.redis.Get(ctx, cacheKey).Result()
	if err == nil {
		var customer Customer
		if err := json.Unmarshal([]byte(cachedCustomer), &customer); err == nil {
			s.logger.Infof("Customer %s retrieved from cache", customerID)
			return &customer, nil
		}
	}

	// Get from memory
	customer, exists := s.customers[customerID]
	if !exists {
		return nil, fmt.Errorf("customer not found: %s", customerID)
	}

	// Cache the customer
	if customerJSON, err := json.Marshal(customer); err == nil {
		s.redis.Set(ctx, cacheKey, customerJSON, 30*time.Minute)
	}

	s.logger.Infof("Customer %s retrieved from memory", customerID)
	return customer, nil
}

// GetAllCustomers returns all customers
func (s *CustomerService) GetAllCustomers() []*Customer {
	customers := make([]*Customer, 0, len(s.customers))
	for _, customer := range s.customers {
		customers = append(customers, customer)
	}
	return customers
}

// CreateCustomer creates a new customer
func (s *CustomerService) CreateCustomer(customer *Customer) error {
	if customer.ID == "" {
		customer.ID = fmt.Sprintf("customer-%s", uuid.New().String()[:8])
	}
	customer.CreatedAt = time.Now().Format(time.RFC3339)
	customer.UpdatedAt = time.Now().Format(time.RFC3339)
	s.customers[customer.ID] = customer
	return nil
}

// UpdateCustomer updates an existing customer
func (s *CustomerService) UpdateCustomer(customerID string, customer *Customer) error {
	if _, exists := s.customers[customerID]; !exists {
		return fmt.Errorf("customer not found: %s", customerID)
	}
	customer.ID = customerID
	customer.UpdatedAt = time.Now().Format(time.RFC3339)
	s.customers[customerID] = customer
	return nil
}

// DeleteCustomer deletes a customer
func (s *CustomerService) DeleteCustomer(customerID string) error {
	if _, exists := s.customers[customerID]; !exists {
		return fmt.Errorf("customer not found: %s", customerID)
	}
	delete(s.customers, customerID)
	return nil
}

// setupRoutes configures the HTTP routes
func setupRoutes(customerService *CustomerService) *gin.Engine {
	r := gin.Default()

	// Health check
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status":  "healthy",
			"service": "customer-service",
			"time":    time.Now().Format(time.RFC3339),
		})
	})

	// API routes
	api := r.Group("/api")
	{
		customers := api.Group("/customers")
		{
			customers.GET("", func(c *gin.Context) {
				customers := customerService.GetAllCustomers()
				c.JSON(http.StatusOK, customers)
			})

			customers.GET("/:id", func(c *gin.Context) {
				customerID := c.Param("id")
				customer, err := customerService.GetCustomer(c.Request.Context(), customerID)
				if err != nil {
					c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
					return
				}
				c.JSON(http.StatusOK, customer)
			})

			customers.POST("", func(c *gin.Context) {
				var customer Customer
				if err := c.ShouldBindJSON(&customer); err != nil {
					c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
					return
				}

				if err := customerService.CreateCustomer(&customer); err != nil {
					c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
					return
				}

				c.JSON(http.StatusCreated, customer)
			})

			customers.PUT("/:id", func(c *gin.Context) {
				customerID := c.Param("id")
				var customer Customer
				if err := c.ShouldBindJSON(&customer); err != nil {
					c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
					return
				}

				if err := customerService.UpdateCustomer(customerID, &customer); err != nil {
					c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
					return
				}

				c.JSON(http.StatusOK, customer)
			})

			customers.DELETE("/:id", func(c *gin.Context) {
				customerID := c.Param("id")
				if err := customerService.DeleteCustomer(customerID); err != nil {
					c.JSON(http.StatusNotFound, gin.H{"error": err.Error()})
					return
				}

				c.JSON(http.StatusOK, gin.H{"message": "Customer deleted successfully"})
			})
		}
	}

	return r
}

func main() {
	// Get port from environment or use default
	port := os.Getenv("PORT")
	if port == "" {
		port = "8082"
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

	// Initialize customer service
	customerService := NewCustomerService(redisClient)

	// Setup routes
	r := setupRoutes(customerService)

	// Start server
	log.Printf("Customer service starting on port %s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
