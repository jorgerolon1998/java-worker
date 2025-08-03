package com.orderprocessor.infrastructure.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Distributed lock service using Redis
 */
@Service
public class DistributedLockService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedLockService.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public DistributedLockService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Acquire a distributed lock
     * @param lockKey the lock key
     * @param ttl the time to live for the lock
     * @return true if lock was acquired, false otherwise
     */
    public Mono<Boolean> acquireLock(String lockKey, Duration ttl) {
        String lockValue = UUID.randomUUID().toString();
        
        return redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, ttl)
                .doOnSuccess(acquired -> {
                    if (acquired) {
                        logger.debug("Lock acquired for key: {}", lockKey);
                    } else {
                        logger.debug("Failed to acquire lock for key: {}", lockKey);
                    }
                })
                .doOnError(error -> logger.error("Error acquiring lock for key {}: {}", lockKey, error.getMessage()));
    }

    /**
     * Release a distributed lock
     * @param lockKey the lock key
     * @return true if lock was released, false otherwise
     */
    public Mono<Boolean> releaseLock(String lockKey) {
        return redisTemplate.delete(lockKey)
                .map(count -> count > 0)
                .doOnSuccess(released -> {
                    if (released) {
                        logger.debug("Lock released for key: {}", lockKey);
                    } else {
                        logger.debug("No lock found to release for key: {}", lockKey);
                    }
                })
                .doOnError(error -> logger.error("Error releasing lock for key {}: {}", lockKey, error.getMessage()));
    }

    /**
     * Check if a lock exists
     * @param lockKey the lock key
     * @return true if lock exists, false otherwise
     */
    public Mono<Boolean> isLocked(String lockKey) {
        return redisTemplate.hasKey(lockKey)
                .doOnSuccess(locked -> logger.debug("Lock check for key {}: {}", lockKey, locked))
                .doOnError(error -> logger.error("Error checking lock for key {}: {}", lockKey, error.getMessage()));
    }

    /**
     * Get remaining TTL for a lock
     * @param lockKey the lock key
     * @return the remaining TTL in seconds, -1 if key doesn't exist, -2 if no TTL
     */
    public Mono<Long> getLockTTL(String lockKey) {
        return redisTemplate.getExpire(lockKey)
                .map(duration -> duration != null ? duration.getSeconds() : -1L)
                .doOnSuccess(ttl -> logger.debug("Lock TTL for key {}: {} seconds", lockKey, ttl))
                .doOnError(error -> logger.error("Error getting lock TTL for key {}: {}", lockKey, error.getMessage()));
    }

    /**
     * Extend the TTL of an existing lock
     * @param lockKey the lock key
     * @param ttl the new time to live
     * @return true if TTL was extended, false if lock doesn't exist
     */
    public Mono<Boolean> extendLock(String lockKey, Duration ttl) {
        return redisTemplate.expire(lockKey, ttl)
                .doOnSuccess(extended -> {
                    if (extended) {
                        logger.debug("Lock TTL extended for key: {}", lockKey);
                    } else {
                        logger.debug("Failed to extend lock TTL for key: {}", lockKey);
                    }
                })
                .doOnError(error -> logger.error("Error extending lock TTL for key {}: {}", lockKey, error.getMessage()));
    }
} 