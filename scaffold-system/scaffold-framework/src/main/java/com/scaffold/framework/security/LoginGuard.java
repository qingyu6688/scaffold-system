package com.scaffold.framework.security;

import com.scaffold.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/** Redis 原子计数，限制匿名接口频率和连续密码失败次数。 */
@Service
public class LoginGuard {
    private static final int LOCK_SECONDS = 900;
    private static final int MAX_FAILURES = 5;
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return count
            """, Long.class);
    @Value("${scaffold.auth.captcha-per-minute:30}")
    private int captchaLimit = 30;
    @Value("${scaffold.auth.login-per-minute:60}")
    private int loginLimit = 60;
    private final StringRedisTemplate redis;

    public LoginGuard(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void checkCaptchaRate(String ip) {
        checkRate("captcha:" + encoded(ip), captchaLimit, 60);
    }

    public void checkLoginRate(String ip) {
        checkRate("login:" + encoded(ip), loginLimit, 60);
    }

    public void checkAccount(String username) {
        String count = redis.opsForValue().get(accountKey(username));
        if (count != null && Long.parseLong(count) >= MAX_FAILURES) {
            throw new BusinessException(429, "密码连续错误次数过多，请15分钟后重试");
        }
    }

    public void failed(String username) {
        redis.execute(INCREMENT, List.of(accountKey(username)), String.valueOf(LOCK_SECONDS));
    }

    public void succeeded(String username) {
        redis.delete(accountKey(username));
    }

    private void checkRate(String suffix, int maximum, int seconds) {
        Long count = redis.execute(INCREMENT, List.of("auth:rate:" + suffix), String.valueOf(seconds));
        if (count == null || count > maximum) {
            throw new BusinessException(429, "请求过于频繁，请稍后重试");
        }
    }

    private String accountKey(String username) {
        // MySQL 默认账号比较不区分大小写，计数键必须采用相同边界。
        return "auth:fail:" + encoded(username.toLowerCase(Locale.ROOT));
    }

    private String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
