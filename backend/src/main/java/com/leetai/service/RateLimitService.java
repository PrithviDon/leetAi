package com.leetai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by Redis. One counter per user per
 * window: INCR on every attempt, TTL set only on the first increment of
 * a fresh window. Simple and battle-tested — not perfectly smooth at
 * window boundaries (a burst right at the edge of two windows can allow
 * up to ~2x the limit briefly), but that tradeoff is fine for protecting
 * against sustained abuse rather than being a precise throttle.
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${ratelimit.submit.max-requests:5}")
    private int maxRequests;

    @Value("${ratelimit.submit.window-seconds:60}")
    private int windowSeconds;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Call once per attempt. Throws RateLimitExceededException if the
     * caller has exceeded maxRequests within the current window.
     */
    public void checkSubmissionAllowed(String userEmail) {
        String key = "ratelimit:submit:" + userEmail;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (count == null || count > maxRequests) {
            Long ttl = redisTemplate.getExpire(key);
            long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;
            throw new com.leetai.exception.RateLimitExceededException(retryAfter);
        }
    }
}
