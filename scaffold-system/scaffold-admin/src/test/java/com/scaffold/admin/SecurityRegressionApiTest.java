package com.scaffold.admin;

import com.scaffold.framework.security.CaptchaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/** 真实数据库和 Redis 的安全回归。必须在独立测试库中运行。 */
@SpringBootTest(classes = ScaffoldAdminApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityRegressionApiTest {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() { };
    private static final String PASSWORD = "TestPwd123!";
    @Autowired private TestRestTemplate http;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private PasswordEncoder encoder;
    @Autowired private CaptchaService captchaService;
    private final List<Long> users = new ArrayList<>();
    private final List<Long> roles = new ArrayList<>();
    private final List<Long> depts = new ArrayList<>();
    private final String suffix = UUID.randomUUID().toString().replace("-", "");
    private HttpHeaders admin;

    @BeforeEach
    void setup() {
        admin = headers(TestAuthClient.login(http, redisTemplate, "admin", "admin123"));
    }

    @AfterEach
    void cleanup() {
        users.forEach(id -> {
            jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", id);
            jdbc.update("DELETE FROM sys_user_post WHERE user_id = ?", id);
            jdbc.update("DELETE FROM sys_login_log WHERE username = (SELECT username FROM sys_user WHERE id = ?)", id);
            jdbc.update("DELETE FROM sys_user WHERE id = ?", id);
        });
        roles.forEach(id -> {
            jdbc.update("DELETE FROM sys_role_menu WHERE role_id = ?", id);
            jdbc.update("DELETE FROM sys_role WHERE id = ?", id);
        });
        Collections.reverse(depts);
        depts.forEach(id -> jdbc.update("DELETE FROM sys_dept WHERE id = ?", id));
    }

    @Test
    void rejectsPrivilegeEscalationAndOutOfScopeWritesBeforeChangingData() {
        long role = role("DEPT");
        long actor = user("actor", 2L, role);
        long outside = user("outside", 8L, null);
        long peer = user("peer", 2L, null);
        HttpHeaders actorHeaders = login(actor);
        assertThat(((List<?>) data(exchange("/api/auth/me", HttpMethod.GET, null, actorHeaders)).get("permissions")).stream().map(String::valueOf).toList())
                .doesNotContain("system:role:add");
        assertStatus(call("/users/" + actor, HttpMethod.PUT, Map.of("roleIds", List.of(1)), actorHeaders), 403);
        assertStatus(call("/users", HttpMethod.POST, Map.of("username", "escalate_" + suffix,
                "nickname", "越权新增", "password", PASSWORD, "deptId", 2, "roleIds", List.of(1)), actorHeaders), 403);
        assertStatus(call("/users/1/password", HttpMethod.PUT, Map.of("password", PASSWORD), actorHeaders), 403);
        assertStatus(call("/users/" + outside, HttpMethod.PUT, Map.of("nickname", "不应写入"), actorHeaders), 403);
        assertStatus(call("/users/" + outside + "/status", HttpMethod.PUT, Map.of("status", 0), actorHeaders), 403);
        assertStatus(call("/users/" + outside + "/password", HttpMethod.PUT, Map.of("password", PASSWORD), actorHeaders), 403);
        assertStatus(call("/users/" + outside, HttpMethod.DELETE, null, actorHeaders), 403);
        assertThat(jdbc.queryForObject("SELECT nickname FROM sys_user WHERE id = ?", String.class, outside)).isEqualTo("安全测试outside");
        assertThat(jdbc.queryForList("SELECT role_id FROM sys_user_role WHERE user_id = ?", Long.class, actor)).containsExactly(role);
        assertStatus(call("/users/" + peer, HttpMethod.PUT, Map.of("nickname", "允许编辑"), actorHeaders), 200);
        assertStatus(call("/roles", HttpMethod.POST, Map.of("name", "越权角色", "code", "blocked_" + suffix), actorHeaders), 403);
    }

    @Test
    void forbidsResettingHigherPrivilegePeerAndAllowsSameDeptRoleDelegation() {
        long role = role("DEPT");
        long actor = user("actor", 2L, role);
        long peer = user("peer", 2L, null);
        long stronger = user("stronger", 2L, 2L);
        var actorHeaders = login(actor);
        assertStatus(call("/users/" + stronger + "/password", HttpMethod.PUT, Map.of("password", PASSWORD), actorHeaders), 403);
        assertStatus(call("/users/" + peer, HttpMethod.PUT, Map.of("roleIds", List.of(role)), actorHeaders), 200);
        assertThat(jdbc.queryForList("SELECT role_id FROM sys_user_role WHERE user_id = ?", Long.class, peer)).containsExactly(role);
    }

    @Test
    void preservesAllUserAssignmentsAndReturnsExplicitIds() {
        long id = user("multi", 2L, 2L);
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, 3)", id);
        jdbc.update("INSERT INTO sys_user_post (user_id, post_id) VALUES (?, 1), (?, 2)", id, id);
        var response = call("/users?keyword=multi_" + suffix, HttpMethod.GET, null, admin);
        assertStatus(response, 200);
        var row = rows(data(response)).get(0);
        assertThat(row.get("deptId")).isEqualTo(2);
        assertThat((List<?>) row.get("roleIds")).hasSize(2);
        assertThat((List<?>) row.get("postIds")).hasSize(2);
        assertStatus(call("/users/" + id, HttpMethod.PUT, Map.of("nickname", "保留关联",
                "roleIds", row.get("roleIds"), "postIds", row.get("postIds")), admin), 200);
        assertThat(jdbc.queryForList("SELECT role_id FROM sys_user_role WHERE user_id = ? ORDER BY role_id", Long.class, id)).containsExactly(2L, 3L);
        assertThat(jdbc.queryForList("SELECT post_id FROM sys_user_post WHERE user_id = ? ORDER BY post_id", Long.class, id)).containsExactly(1L, 2L);
    }

    @Test
    void validatesMissingPasswordsStatusesAndForeignIds() {
        assertStatus(call("/configs", HttpMethod.POST, Map.of("name", "非法类型", "key", "bad_" + suffix, "value", "1", "type", "invalid"), admin), 400);
        var base = new HashMap<String, Object>(Map.of("username", "invalid_" + suffix, "nickname", "参数校验", "deptId", 2));
        assertStatus(call("/users", HttpMethod.POST, base, admin), 400);
        base.put("password", PASSWORD);
        base.put("status", 7);
        assertStatus(call("/users", HttpMethod.POST, base, admin), 400);
        base.put("status", 1);
        base.put("roleIds", List.of(999999999));
        assertStatus(call("/users", HttpMethod.POST, base, admin), 400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, base.get("username"))).isZero();
        long id = user("valid", 2L, null);
        assertStatus(call("/users/" + id, HttpMethod.PUT, Map.of("status", 7), admin), 400);
        assertStatus(call("/users/" + id + "/password", HttpMethod.PUT, Map.of(), admin), 400);
        assertStatus(call("/users/1", HttpMethod.PUT, Map.of("status", 0), admin), 400);
        assertStatus(call("/users/1", HttpMethod.PUT, Map.of("roleIds", List.of()), admin), 400);
    }

    @Test
    void passwordChangesInvalidateEveryOldSession() {
        long id = user("password", 2L, null);
        var first = login(id);
        var second = login(id);
        assertStatus(exchange("/api/auth/password", HttpMethod.PUT,
                Map.of("oldPassword", PASSWORD, "newPassword", "NewPwd123!"), first), 200);
        assertUnauthorized(first);
        assertUnauthorized(second);
        String username = jdbc.queryForObject("SELECT username FROM sys_user WHERE id = ?", String.class, id);
        assertThat(TestAuthClient.login(http, redisTemplate, username, "NewPwd123!")).isNotBlank();
    }

    @Test
    void resetDisableAndDeleteInvalidateSessionsIncludingAfterReenable() {
        long id = user("revoke", 2L, null);
        var old = login(id);
        assertStatus(call("/users/" + id + "/password", HttpMethod.PUT, Map.of("password", PASSWORD), admin), 200);
        assertUnauthorized(old);
        var current = login(id);
        assertStatus(call("/users/" + id + "/status", HttpMethod.PUT, Map.of("status", 0), admin), 200);
        assertUnauthorized(current);
        assertStatus(call("/users/" + id + "/status", HttpMethod.PUT, Map.of("status", 1), admin), 200);
        assertUnauthorized(current);
        var last = login(id);
        assertStatus(call("/users/" + id, HttpMethod.DELETE, null, admin), 200);
        assertUnauthorized(last);
    }

    @Test
    void departmentMoveUpdatesAllDescendantsAndRejectsCycles() {
        long first = dept(1L);
        long child = dept(first);
        long grandchild = dept(child);
        long destination = dept(1L);
        assertStatus(call("/depts/" + first, HttpMethod.PUT, Map.of("parentId", grandchild), admin), 400);
        assertStatus(call("/depts/" + first, HttpMethod.PUT, Map.of("parentId", first), admin), 400);
        assertStatus(call("/depts/" + first, HttpMethod.PUT, Map.of("parentId", destination), admin), 200);
        assertThat(jdbc.queryForObject("SELECT ancestors FROM sys_dept WHERE id = ?", String.class, grandchild))
                .isEqualTo("1," + destination + "," + first + "," + child);
    }

    @Test
    void captchaReturnsPngAndCannotBeConsumedTwiceEvenConcurrently() throws Exception {
        var challenge = data(exchange("/api/auth/captcha", HttpMethod.GET, null, null));
        assertThat(challenge).containsKeys("uuid", "image").doesNotContainKey("code");
        String png = (String) challenge.get("image");
        assertThat(ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(png.substring(png.indexOf(',') + 1)))).getWidth()).isEqualTo(140);
        String uuid = (String) challenge.get("uuid");
        String code = redisTemplate.opsForValue().get("captcha:" + uuid);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> captchaService.verify(uuid, code));
            var second = pool.submit(() -> captchaService.verify(uuid, code));
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void locksAccountAfterFiveWrongPasswordsAcrossUsernameCase() {
        long id = user("locked", 2L, null);
        String username = jdbc.queryForObject("SELECT username FROM sys_user WHERE id = ?", String.class, id);
        for (int i = 0; i < 5; i++) {
            assertStatus(TestAuthClient.loginResponse(http, redisTemplate, username, "WrongPwd123!"), 400);
        }
        assertStatus(TestAuthClient.loginResponse(http, redisTemplate, username.toUpperCase(Locale.ROOT), PASSWORD), 429);
    }

    private long role(String scope) {
        String code = "security_" + suffix;
        jdbc.update("INSERT INTO sys_role (role_name, role_code, data_scope) VALUES ('安全回归角色', ?, ?)", code, scope);
        Long id = jdbc.queryForObject("SELECT id FROM sys_role WHERE role_code = ?", Long.class, code);
        roles.add(id);
        jdbc.update("INSERT INTO sys_role_menu (role_id, menu_id) SELECT ?, id FROM sys_menu WHERE permission IN ('system:user:list','system:user:add','system:user:edit','system:user:remove','system:user:resetPwd','system:role:add')", id);
        return id;
    }

    private long user(String name, Long deptId, Long roleId) {
        String username = name + "_" + suffix;
        jdbc.update("INSERT INTO sys_user (username, password, nickname, dept_id) VALUES (?, ?, ?, ?)", username, encoder.encode(PASSWORD), "安全测试" + name, deptId);
        Long id = jdbc.queryForObject("SELECT id FROM sys_user WHERE username = ?", Long.class, username);
        users.add(id);
        if (roleId != null) jdbc.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", id, roleId);
        return id;
    }

    private long dept(long parent) {
        var response = call("/depts", HttpMethod.POST, Map.of("name", "安全回归部门", "parentId", parent), admin);
        assertStatus(response, 200);
        long id = ((Number) data(response).get("id")).longValue();
        depts.add(id);
        return id;
    }

    private HttpHeaders login(long id) {
        String username = jdbc.queryForObject("SELECT username FROM sys_user WHERE id = ?", String.class, id);
        return headers(TestAuthClient.login(http, redisTemplate, username, PASSWORD));
    }

    private HttpHeaders headers(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private void assertUnauthorized(HttpHeaders headers) {
        assertStatus(exchange("/api/auth/me", HttpMethod.GET, null, headers), 401);
        assertStatus(exchange("/api/dashboard/summary", HttpMethod.GET, null, headers), 401);
    }

    private ResponseEntity<Map<String, Object>> call(String path, HttpMethod method, Object body, HttpHeaders headers) {
        return exchange("/api/system" + path, method, body, headers);
    }

    private ResponseEntity<Map<String, Object>> exchange(String path, HttpMethod method, Object body, HttpHeaders headers) {
        return http.exchange(path, method, new HttpEntity<>(body, headers), MAP);
    }

    private void assertStatus(ResponseEntity<Map<String, Object>> response, int status) {
        assertThat(response.getStatusCode().value()).withFailMessage("响应：%s", response.getBody()).isEqualTo(status);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map<String, Object>> response) {
        return (Map<String, Object>) response.getBody().get("data");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> rows(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("rows");
    }
}
