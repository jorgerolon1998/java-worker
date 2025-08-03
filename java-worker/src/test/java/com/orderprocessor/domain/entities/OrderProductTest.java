package com.orderprocessor.domain.entities;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderProductTest {

    @Test
    void testOrderProductCreation() {
        OrderProduct product = new OrderProduct("product-001", "Laptop Gaming", new BigDecimal("999.99"));
        
        assertEquals("product-001", product.getProductId());
        assertEquals("Laptop Gaming", product.getName());
        assertEquals(new BigDecimal("999.99"), product.getPrice());
        assertTrue(product.isActive());
    }

    @Test
    void testOrderProductWithDescription() {
        OrderProduct product = new OrderProduct("product-002", "Mouse", "Gaming Mouse", new BigDecimal("29.99"));
        
        assertEquals("product-002", product.getProductId());
        assertEquals("Mouse", product.getName());
        assertEquals("Gaming Mouse", product.getDescription());
        assertEquals(new BigDecimal("29.99"), product.getPrice());
    }

    @Test
    void testSetAndGetMethods() {
        OrderProduct product = new OrderProduct();
        
        product.setProductId("product-003");
        product.setName("Keyboard");
        product.setDescription("Mechanical Keyboard");
        product.setPrice(new BigDecimal("149.99"));
        product.setActive(false);
        
        assertEquals("product-003", product.getProductId());
        assertEquals("Keyboard", product.getName());
        assertEquals("Mechanical Keyboard", product.getDescription());
        assertEquals(new BigDecimal("149.99"), product.getPrice());
        assertFalse(product.isActive());
    }

    @Test
    void testToString() {
        OrderProduct product = new OrderProduct("product-001", "Laptop", new BigDecimal("999.99"));
        String toString = product.toString();
        
        assertTrue(toString.contains("product-001"));
        assertTrue(toString.contains("Laptop"));
        assertTrue(toString.contains("999.99"));
    }
} 