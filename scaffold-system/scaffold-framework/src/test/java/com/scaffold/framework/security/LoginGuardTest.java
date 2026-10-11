package com.scaffold.framework.security;

import com.scaffold.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginGuardTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final LoginGuard guard = new LoginGuard(redis);

    @BeforeEach
    void setup() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void limitsCaptchaAndLoginUsingAtomicCounters() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(31L);
        assertThatThrownBy(() -> guard.checkCaptchaRate("127.0.0.1")).isInstanceOf(BusinessException.class);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(61L);
        assertThatThrownBy(() -> guard.checkLoginRate("127.0.0.1")).isInstanceOf(BusinessException.class);
    }

    @Test
    void locksAccountAndUsesTheSameKeyForDifferentLetterCase() {
        when(values.get(anyString())).thenReturn("5");
        assertThatThrownBy(() -> guard.checkAccount("ADMIN")).isInstanceOf(BusinessException.class);
        guard.succeeded("admin");
        verify(values).get("auth:fail:YWRtaW4");
        verify(redis).delete("auth:fail:YWRtaW4");
    }
}
