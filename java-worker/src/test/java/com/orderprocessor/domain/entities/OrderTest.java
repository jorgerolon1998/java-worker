package com.orderprocessor.domain.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    private Order order;
    private List<OrderProduct> products;

    @BeforeEach
    void setUp() {
        products = Arrays.asList(
            new OrderProduct("product-001", "Laptop", new BigDecimal("999.99")),
            new OrderProduct("product-002", "Mouse", new BigDecimal("29.99"))
        );
        
        order = new Order("order-123", "customer-456", products);
    }

    @Test
    void testOrderCreation() {
        assertNotNull(order);
        assertEquals("order-123", order.getOrderId());
        assertEquals("customer-456", order.getCustomerId());
        assertEquals(2, order.getProducts().size());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void testCalculateTotalAmount() {
        BigDecimal expectedTotal = new BigDecimal("1029.98");
        assertEquals(expectedTotal, order.calculateTotalAmount());
    }

    @Test
    void testMarkAsCompleted() {
        order.markAsCompleted();
        assertEquals(OrderStatus.COMPLETED, order.getStatus());
    }

    @Test
    void testMarkAsFailed() {
        order.markAsFailed();
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    @Test
    void testEnrichWithCustomerDetails() {
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setCustomerId("customer-456");
        customerDetails.setName("John Doe");
        customerDetails.setEmail("john@example.com");
        customerDetails.setStatus(CustomerStatus.ACTIVE);

        order.enrichWithCustomerDetails(customerDetails);

        assertNotNull(order.getCustomerDetails());
        assertEquals("John Doe", order.getCustomerDetails().getName());
        assertEquals("john@example.com", order.getCustomerDetails().getEmail());
    }
} 