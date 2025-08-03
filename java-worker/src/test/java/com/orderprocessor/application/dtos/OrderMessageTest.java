package com.orderprocessor.application.dtos;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderMessageTest {

    @Test
    void testOrderMessageCreation() {
        List<String> productIds = Arrays.asList("product-001", "product-002");
        OrderMessage message = new OrderMessage("order-123", "customer-456", productIds);
        
        assertEquals("order-123", message.getOrderId());
        assertEquals("customer-456", message.getCustomerId());
        assertEquals(2, message.getProductIds().size());
        assertEquals("product-001", message.getProductIds().get(0));
        assertEquals("product-002", message.getProductIds().get(1));
    }

    @Test
    void testOrderMessageWithTimestamp() {
        List<String> productIds = Arrays.asList("product-001");
        LocalDateTime timestamp = LocalDateTime.now();
        
        OrderMessage message = new OrderMessage("order-123", "customer-456", productIds);
        message.setTimestamp(timestamp);
        
        assertEquals("order-123", message.getOrderId());
        assertEquals("customer-456", message.getCustomerId());
        assertEquals(1, message.getProductIds().size());
        assertEquals(timestamp, message.getTimestamp());
    }

    @Test
    void testSetAndGetMethods() {
        OrderMessage message = new OrderMessage();
        
        message.setOrderId("order-456");
        message.setCustomerId("customer-789");
        message.setProductIds(Arrays.asList("product-003", "product-004"));
        
        assertEquals("order-456", message.getOrderId());
        assertEquals("customer-789", message.getCustomerId());
        assertEquals(2, message.getProductIds().size());
        assertEquals("product-003", message.getProductIds().get(0));
        assertEquals("product-004", message.getProductIds().get(1));
    }

    @Test
    void testToString() {
        List<String> productIds = Arrays.asList("product-001");
        OrderMessage message = new OrderMessage("order-123", "customer-456", productIds);
        String toString = message.toString();
        
        assertTrue(toString.contains("order-123"));
        assertTrue(toString.contains("customer-456"));
        assertTrue(toString.contains("product-001"));
    }
} 