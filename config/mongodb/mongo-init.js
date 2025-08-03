// MongoDB initialization script
// This script runs when the MongoDB container starts for the first time

// Switch to the orders database
db = db.getSiblingDB('orders');

// Create collections with validation
db.createCollection("orders", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["orderId", "customerId", "products", "createdAt"],
      properties: {
        orderId: {
          bsonType: "string",
          description: "must be a string and is required"
        },
        customerId: {
          bsonType: "string",
          description: "must be a string and is required"
        },
        products: {
          bsonType: "array",
          description: "must be an array and is required",
          items: {
            bsonType: "object",
            required: ["productId", "name", "price"],
            properties: {
              productId: {
                bsonType: "string"
              },
              name: {
                bsonType: "string"
              },
              price: {
                bsonType: "number"
              },
              description: {
                bsonType: "string"
              }
            }
          }
        },
        totalAmount: {
          bsonType: "number"
        },
        status: {
          bsonType: "string",
          enum: ["pending", "processing", "completed", "failed"]
        },
        createdAt: {
          bsonType: "date"
        },
        updatedAt: {
          bsonType: "date"
        }
      }
    }
  }
});

// Create indexes for better performance
db.orders.createIndex({ "orderId": 1 }, { unique: true });
db.orders.createIndex({ "customerId": 1 });
db.orders.createIndex({ "createdAt": 1 });
db.orders.createIndex({ "status": 1 });

// Create collection for failed messages
db.createCollection("failed_messages", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["orderId", "error", "retryCount", "createdAt"],
      properties: {
        orderId: {
          bsonType: "string"
        },
        originalMessage: {
          bsonType: "object"
        },
        error: {
          bsonType: "string"
        },
        retryCount: {
          bsonType: "int"
        },
        maxRetries: {
          bsonType: "int"
        },
        createdAt: {
          bsonType: "date"
        },
        lastRetryAt: {
          bsonType: "date"
        }
      }
    }
  }
});

// Create indexes for failed messages
db.failed_messages.createIndex({ "orderId": 1 });
db.failed_messages.createIndex({ "retryCount": 1 });
db.failed_messages.createIndex({ "createdAt": 1 });

print("MongoDB initialized successfully!");
print("Database: orders");
print("Collections: orders, failed_messages");
print("Indexes created for better performance"); 