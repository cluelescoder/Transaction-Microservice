package com.lloyds.transaction.service.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private RedisService redisService;

    private static final String BLACKLISTED_JTI = "blacklistedToken";
    private static final String NON_BLACKLISTED_JTI = "validToken";


    @Test
    void testIsTokenBlacklisted_whenRedisAvailable() {
        when(redisTemplate.hasKey(BLACKLISTED_JTI)).thenReturn(true);
        assertThat(redisService.isTokenBlacklisted(BLACKLISTED_JTI)).isTrue();

        when(redisTemplate.hasKey(NON_BLACKLISTED_JTI)).thenReturn(false);
        assertThat(redisService.isTokenBlacklisted(NON_BLACKLISTED_JTI)).isFalse();
    }


    @Test
    void testIsTokenBlacklisted_whenTokenNotBlacklisted() {
        doThrow(new RuntimeException("Redis unavailable")).when(redisTemplate).hasKey(anyString());
        assertThat(redisService.isTokenBlacklisted(NON_BLACKLISTED_JTI)).isFalse();
    }
}
