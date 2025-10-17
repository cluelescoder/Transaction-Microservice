package com.lloyds.transaction.service.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Slf4j
@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public boolean isTokenBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(jti));
        } catch (Exception e) {
            log.error("Redis is unavailable, checking local cache: {}", e.getMessage());
            return false;
        }
    }
}

