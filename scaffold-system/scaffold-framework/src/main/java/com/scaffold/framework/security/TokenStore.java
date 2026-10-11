package com.scaffold.framework.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class TokenStore {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";

    private final StringRedisTemplate redisTemplate;

    public TokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void store(String token, String username, long expiresSeconds) {
        store(token, username, expiresSeconds, 0);
    }

    public void store(String token, String username, long expiresSeconds, long version) {
        redisTemplate.opsForValue().set(key(token), username + "\n" + version, expiresSeconds, TimeUnit.SECONDS);
    }

    public Optional<String> usernameOf(String token) {
        return sessionOf(token).map(Session::username);
    }

    public Optional<Session> sessionOf(String token) {
        String value = redisTemplate.opsForValue().get(key(token));
        if (value == null) return Optional.empty();
        String[] parts = value.split("\n", -1);
        // 升级前未绑定会话版本的令牌必须重新登录。
        if (parts.length != 2 || !parts[1].matches("[0-9]+")) return Optional.empty();
        try {
            return Optional.of(new Session(parts[0], Long.parseLong(parts[1])));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public record Session(String username, long version) {
    }

    public void revoke(String token) {
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return TOKEN_KEY_PREFIX + token;
    }
}
