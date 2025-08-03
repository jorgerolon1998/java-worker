package com.orderprocessor.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redis cache service for reactive operations
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public CacheService(ReactiveRedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Set a value in cache with TTL
     * @param key the cache key
     * @param value the value to cache
     * @param ttl the time to live
     * @return Mono<Void>
     */
    public <T> Mono<Void> set(String key, T value, Duration ttl) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
                    return redisTemplate.opsForValue()
                .set(key, jsonValue, ttl)
                .then()
                .doOnSuccess(v -> logger.debug("Cached value for key: {}", key))
                .doOnError(error -> logger.error("Error caching value for key {}: {}", key, error.getMessage()));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing value for key {}: {}", key, e.getMessage());
            return Mono.error(e);
        }
    }

    /**
     * Get a value from cache
     * @param key the cache key
     * @param clazz the class type
     * @return the cached value or empty if not found
     */
    public <T> Mono<T> get(String key, Class<T> clazz) {
        return redisTemplate.opsForValue()
                .get(key)
                .flatMap(jsonValue -> {
                    try {
                        T value = objectMapper.readValue(jsonValue, clazz);
                        return Mono.just(value);
                    } catch (JsonProcessingException e) {
                        logger.error("Error deserializing value for key {}: {}", key, e.getMessage());
                        return Mono.error(e);
                    }
                })
                .doOnSuccess(value -> logger.debug("Retrieved cached value for key: {}", key))
                .doOnError(error -> logger.error("Error retrieving cached value for key {}: {}", key, error.getMessage()));
    }

    /**
     * Delete a value from cache
     * @param key the cache key
     * @return Mono<Boolean> true if deleted, false if not found
     */
    public Mono<Boolean> delete(String key) {
        return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(deleted -> {
                    if (deleted) {
                        logger.debug("Deleted cached value for key: {}", key);
                    } else {
                        logger.debug("No cached value found for key: {}", key);
                    }
                })
                .doOnError(error -> logger.error("Error deleting cached value for key {}: {}", key, error.getMessage()));
    }

    /**
     * Check if a key exists in cache
     * @param key the cache key
     * @return Mono<Boolean> true if exists, false otherwise
     */
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
                .doOnSuccess(exists -> logger.debug("Key {} exists in cache: {}", key, exists))
                .doOnError(error -> logger.error("Error checking key existence for {}: {}", key, error.getMessage()));
    }

    /**
     * Set TTL for an existing key
     * @param key the cache key
     * @param ttl the time to live
     * @return Mono<Boolean> true if TTL was set, false if key doesn't exist
     */
    public Mono<Boolean> expire(String key, Duration ttl) {
        return redisTemplate.expire(key, ttl)
                .doOnSuccess(expired -> {
                    if (expired) {
                        logger.debug("Set TTL for key: {}", key);
                    } else {
                        logger.debug("Key {} not found for TTL setting", key);
                    }
                })
                .doOnError(error -> logger.error("Error setting TTL for key {}: {}", key, error.getMessage()));
    }
} 