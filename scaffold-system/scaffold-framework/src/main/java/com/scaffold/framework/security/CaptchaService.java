package com.scaffold.framework.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final long EXPIRE_SECONDS = 300;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final int WIDTH = 140;
    private static final int HEIGHT = 48;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public CaptchaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Map<String, String> generate() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + uuid, code.toString(), EXPIRE_SECONDS, TimeUnit.SECONDS);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("uuid", uuid);
        result.put("image", render(code.toString()));
        return result;
    }

    public boolean verify(String uuid, String code) {
        if (uuid == null || !uuid.matches("[a-f0-9]{32}") || code == null || code.length() != CODE_LENGTH) return false;
        String key = KEY_PREFIX + uuid;
        String stored = redisTemplate.opsForValue().getAndDelete(key);
        if (stored == null) return false;
        return stored.equalsIgnoreCase(code);
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(245, 247, 250));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
            for (int i = 0; i < CODE_LENGTH; i++) {
                graphics.setColor(new Color(35 + RANDOM.nextInt(80), 45 + RANDOM.nextInt(80), 65 + RANDOM.nextInt(80)));
                double rotation = (RANDOM.nextDouble() - 0.5) * 0.5;
                int x = 14 + i * 30;
                graphics.rotate(rotation, x, 30);
                graphics.drawString(String.valueOf(code.charAt(i)), x, 34 + RANDOM.nextInt(5));
                graphics.rotate(-rotation, x, 30);
            }
            for (int i = 0; i < 6; i++) {
                graphics.setColor(new Color(140 + RANDOM.nextInt(70), 140 + RANDOM.nextInt(70), 140 + RANDOM.nextInt(70)));
                graphics.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", bytes);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("验证码图片生成失败", ex);
        } finally {
            graphics.dispose();
        }
    }
}
