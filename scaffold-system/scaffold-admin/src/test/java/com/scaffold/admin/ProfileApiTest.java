package com.scaffold.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ScaffoldAdminApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ProfileApiTest {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };
    private static final String PASSWORD = "profilePwd123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String username;

    @BeforeEach
    void setup() {
        // 无任何角色和菜单的普通账号：个人中心不应依赖菜单或按钮权限
        username = "profile_" + Long.toString(System.nanoTime(), 36);
        jdbcTemplate.update("""
                        INSERT INTO sys_user (username, password, nickname, phone, email, dept_id, status, deleted, create_by, update_by)
                        VALUES (?, ?, ?, ?, ?, 5, 1, 0, 'it', 'it')
                        """,
                username, passwordEncoder.encode(PASSWORD), "资料测试用户", "13800000000", "old@example.com");
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM sys_user WHERE username = ?", username);
        jdbcTemplate.update("DELETE FROM sys_login_log WHERE username = ?", username);
        jdbcTemplate.update("DELETE FROM sys_oper_log WHERE oper_name = ?", username);
    }

    @Test
    void userWithoutMenusCanReadOwnProfileAndRecentLogins() {
        HttpHeaders headers = bearerHeaders(login());

        ResponseEntity<Map<String, Object>> response = exchange(HttpMethod.GET, null, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = dataOf(response.getBody());
        assertThat(data).containsEntry("username", username)
                .containsEntry("nickname", "资料测试用户")
                .containsEntry("phone", "13800000000")
                .containsEntry("email", "old@example.com")
                .containsKeys("deptName", "roles", "posts", "lastLoginTime", "createTime");
        assertThat(data.get("lastLoginTime")).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> logins = (List<Map<String, Object>>) data.get("recentLogins");
        assertThat(logins).isNotEmpty();
        assertThat(logins.get(0)).containsEntry("status", 1).containsKey("time");
    }

    @Test
    void updatesOnlyOwnContactFieldsAndKeepsCurrentSession() {
        String token = login();
        HttpHeaders headers = bearerHeaders(token);
        Long deptBefore = jdbcTemplate.queryForObject("SELECT dept_id FROM sys_user WHERE username = ?", Long.class, username);
        Long versionBefore = jdbcTemplate.queryForObject("SELECT session_version FROM sys_user WHERE username = ?", Long.class, username);

        Map<String, Object> body = new HashMap<>();
        body.put("nickname", "  新的姓名 ");
        body.put("phone", "");
        body.put("email", "new@example.com");
        // 未开放的字段即使传入也不能被改写
        body.put("username", "hijacked");
        body.put("deptId", 1);
        body.put("status", 0);
        ResponseEntity<Map<String, Object>> response = exchange(HttpMethod.PUT, body, headers);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> data = dataOf(response.getBody());
        assertThat(data).containsEntry("nickname", "新的姓名").containsEntry("email", "new@example.com");
        assertThat(data.get("phone")).isNull();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT username, nickname, phone, email, dept_id, status, session_version, update_by FROM sys_user WHERE username = ?", username);
        assertThat(row.get("nickname")).isEqualTo("新的姓名");
        assertThat(row.get("phone")).isNull();
        assertThat(row.get("email")).isEqualTo("new@example.com");
        assertThat(((Number) row.get("dept_id")).longValue()).isEqualTo(deptBefore);
        assertThat(((Number) row.get("status")).intValue()).isEqualTo(1);
        assertThat(((Number) row.get("session_version")).longValue()).isEqualTo(versionBefore);
        assertThat(row.get("update_by")).isEqualTo(username);

        ResponseEntity<Map<String, Object>> me = restTemplate.exchange("/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), MAP_TYPE);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(dataOf(me.getBody())).containsEntry("nickname", "新的姓名");

        Integer logs = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE oper_name = ? AND module = '个人中心' AND action = '修改资料'", Integer.class, username);
        assertThat(logs).isEqualTo(1);
    }

    @Test
    void rejectsInvalidProfileWithoutChangingData() {
        HttpHeaders headers = bearerHeaders(login());

        assertBadRequest(exchange(HttpMethod.PUT, Map.of("nickname", "  ", "email", "a@example.com"), headers));
        assertBadRequest(exchange(HttpMethod.PUT, Map.of("nickname", "姓名", "email", "not-an-email"), headers));
        assertBadRequest(exchange(HttpMethod.PUT, Map.of("nickname", "姓名", "phone", "phone=1;"), headers));
        assertBadRequest(exchange(HttpMethod.PUT, Map.of("nickname", "名".repeat(65)), headers));

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT nickname, phone, email FROM sys_user WHERE username = ?", username);
        assertThat(row).containsEntry("nickname", "资料测试用户")
                .containsEntry("phone", "13800000000")
                .containsEntry("email", "old@example.com");
    }

    @Test
    void anonymousRequestIsUnauthorized() {
        ResponseEntity<Map<String, Object>> response = exchange(HttpMethod.GET, null, new HttpHeaders());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map<String, Object>> exchange(HttpMethod method, Object body, HttpHeaders headers) {
        return restTemplate.exchange("/api/profile", method, new HttpEntity<>(body, headers), MAP_TYPE);
    }

    private void assertBadRequest(ResponseEntity<Map<String, Object>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", 400);
    }

    private String login() {
        return TestAuthClient.login(restTemplate, redisTemplate, username, PASSWORD);
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(Map<String, Object> body) {
        assertThat(body).containsEntry("code", 200);
        return (Map<String, Object>) body.get("data");
    }
}
