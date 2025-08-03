package com.orderprocessor.application.usecases;

import com.orderprocessor.application.dtos.OrderMessage;
import com.orderprocessor.domain.entities.*;
import com.orderprocessor.domain.repositories.OrderRepository;
import com.orderprocessor.infrastructure.external.CustomerServiceClient;
import com.orderprocessor.infrastructure.external.ProductServiceClient;
import com.orderprocessor.infrastructure.redis.CacheService;
import com.orderprocessor.infrastructure.redis.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessOrderUseCaseTest {

    @Mock
    private ProductServiceClient productServiceClient;

    @Mock
    private CustomerServiceClient customerServiceClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CacheService cacheService;

    @Mock
    private DistributedLockService distributedLockService;

    private ProcessOrderUseCase processOrderUseCase;

    @BeforeEach
    void setUp() {
        processOrderUseCase = new ProcessOrderUseCase(
            orderRepository,
            productServiceClient,
            customerServiceClient,
            distributedLockService,
            cacheService
        );
    }

    @Test
    void testProcessOrderSuccess() {
        // Arrange
        OrderMessage orderMessage = new OrderMessage("order-123", "customer-456", 
            Arrays.asList("product-001", "product-002"));
        
        OrderProduct product1 = new OrderProduct("product-001", "Laptop", new BigDecimal("999.99"));
        OrderProduct product2 = new OrderProduct("product-002", "Mouse", new BigDecimal("29.99"));
        
        CustomerDetails customerDetails = new CustomerDetails();
        customerDetails.setCustomerId("customer-456");
        customerDetails.setName("John Doe");
        customerDetails.setStatus(CustomerStatus.ACTIVE);
        customerDetails.setCreditLimit(new BigDecimal("5000"));
        customerDetails.setCurrentBalance(new BigDecimal("1000"));
        
        Order savedOrder = new Order("order-123", "customer-456", Arrays.asList(product1, product2));
        savedOrder.enrichWithCustomerDetails(customerDetails);
        
        // Mock distributed lock
        when(distributedLockService.acquireLock(anyString(), any(Duration.class)))
            .thenReturn(Mono.just(true));
        when(distributedLockService.releaseLock(anyString()))
            .thenReturn(Mono.empty());
        
        // Mock order repository
        when(orderRepository.existsByOrderId("order-123"))
            .thenReturn(Mono.just(false));
        when(orderRepository.save(any(Order.class)))
            .thenReturn(Mono.just(savedOrder));
        
        // Mock product service calls
        when(productServiceClient.getProduct("product-001"))
            .thenReturn(Mono.just(product1));
        when(productServiceClient.getProduct("product-002"))
            .thenReturn(Mono.just(product2));
        
        // Mock customer service call
        when(customerServiceClient.getCustomer("customer-456"))
            .thenReturn(Mono.just(customerDetails));
        
        // Mock cache calls
        when(cacheService.get(anyString(), eq(OrderProduct.class)))
            .thenReturn(Mono.empty());
        when(cacheService.set(anyString(), any(OrderProduct.class), any(Duration.class)))
            .thenReturn(Mono.empty());
        when(cacheService.get(anyString(), eq(CustomerDetails.class)))
            .thenReturn(Mono.empty());
        when(cacheService.set(anyString(), any(CustomerDetails.class), any(Duration.class)))
            .thenReturn(Mono.empty());
        
        // Act & Assert
        StepVerifier.create(processOrderUseCase.processOrder(orderMessage))
            .expectNextMatches(order -> 
                order.getOrderId().equals("order-123") &&
                order.getCustomerId().equals("customer-456") &&
                order.getProducts().size() == 2 &&
                order.getCustomerDetails() != null
            )
            .verifyComplete();
        
        // Verify interactions
        verify(distributedLockService).acquireLock(contains("order-123"), any(Duration.class));
        verify(distributedLockService).releaseLock(contains("order-123"));
        verify(productServiceClient, times(2)).getProduct(anyString());
        verify(customerServiceClient).getCustomer("customer-456");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testProcessOrderWithLockFailure() {
        // Arrange
        OrderMessage orderMessage = new OrderMessage("order-123", "customer-456", 
            Arrays.asList("product-001"));
        
        // Mock distributed lock failure
        when(distributedLockService.acquireLock(anyString(), any(Duration.class)))
            .thenReturn(Mono.just(false));
        
        // Act & Assert
        StepVerifier.create(processOrderUseCase.processOrder(orderMessage))
            .verifyComplete();
        
        // Verify no further interactions
        verify(productServiceClient, never()).getProduct(anyString());
        verify(customerServiceClient, never()).getCustomer(anyString());
        verify(orderRepository, never()).save(any(Order.class));
    }
} 