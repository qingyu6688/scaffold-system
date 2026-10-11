"""本地浏览器回归：登录竞态、菜单与按钮权限、旧账号请求晚到。

需要已安装的 Python Playwright/Chromium 与运行中的前后端。
创建并删除自己生成的测试角色/用户，建议连接独立校验库；不清空任何表。
密码通过 SCAFFOLD_TEST_PASSWORD 传入，本地种子库默认使用开发账号。
"""
import argparse
import asyncio
import os
import re
import secrets
import socket
import time
from pathlib import Path
from urllib.parse import urlparse

from playwright.async_api import async_playwright, expect


def test_captcha(uuid):
    """测试专用 RESP 客户端；必须显式提供回环地址上的独立测试实例端口。"""
    port = os.environ.get("SCAFFOLD_TEST_REDIS_PORT")
    if not port:
        raise RuntimeError("请先设置 SCAFFOLD_TEST_REDIS_PORT，脚本仅适用于独立测试环境")
    with socket.create_connection(("127.0.0.1", int(port)), timeout=5) as connection:
        reader = connection.makefile("rb")
        def command(*parts):
            encoded = [str(part).encode() for part in parts]
            connection.sendall(b"*%d\r\n" % len(encoded) + b"".join(b"$%d\r\n" % len(part) + part + b"\r\n" for part in encoded))
            line = reader.readline()
            if line.startswith(b"+"): return line[1:-2].decode()
            if line.startswith(b"$") and int(line[1:-2]) >= 0:
                size = int(line[1:-2]); value = reader.read(size); reader.read(2)
                return value.decode()
            raise RuntimeError("测试 Redis 读取失败，请检查实例、密码及数据库编号")
        password = os.environ.get("SCAFFOLD_TEST_REDIS_PASSWORD")
        if password: command("AUTH", password)
        command("SELECT", os.environ.get("SCAFFOLD_TEST_REDIS_DATABASE", "0"))
        return command("GET", "captcha:" + uuid)


async def sign_in(page, username, password, destination=None):
    await page.wait_for_url("**/login**")
    # 等初始验证码请求结束，再显式刷新，避免捕获上一次仍在途的响应。
    await page.wait_for_load_state("networkidle")
    await page.get_by_role("button", name="登录", exact=True).wait_for()
    await expect(page.get_by_role("button", name="登录", exact=True)).to_be_enabled()
    async with page.expect_request("**/api/auth/captcha") as next_captcha:
        await page.get_by_role("button", name="看不清，换一张", exact=True).click()
    captcha_request = await next_captcha.value
    captcha_response = await (await captcha_request.response()).json()
    await expect(page.get_by_role("button", name="看不清，换一张", exact=True)).to_be_enabled()
    captcha = test_captcha(captcha_response["data"]["uuid"])
    await page.get_by_placeholder("请输入账号", exact=True).fill(username)
    await page.get_by_placeholder("请输入密码", exact=True).fill(password)
    await page.get_by_placeholder("请输入右侧字符", exact=True).fill(captcha)
    await page.get_by_role("button", name="登录", exact=True).click()
    if destination:
        try:
            await page.wait_for_url("**" + destination, timeout=10000)
        except Exception:
            print("登录落点异常：", page.url, "提示：", await page.locator(".login-error, .field-error, .toast").all_text_contents())
            raise
        await page.locator("main").wait_for(state="visible")


async def sign_out(page):
    await page.locator(".user-chip").click()
    await page.get_by_role("menuitem", name="退出登录").click()
    await page.get_by_role("dialog").get_by_role("button", name="退出", exact=True).click()
    await page.wait_for_url("**/login**")


async def request_health(page, wait=True):
    # 使用应用实际加载的模块 URL，避免 Vite 的版本参数形成第二个 HTTP 模块实例。
    await page.evaluate("""async (wait) => {
      const source = performance.getEntriesByType('resource').find(entry => entry.name.includes('/src/api/http.ts'));
      const {httpClient} = await import(source.name);
      const request = httpClient.get('/health').catch(() => {});
      if (wait) await request;
    }""", wait)


async def main(base_url):
    password = os.environ.get("SCAFFOLD_TEST_PASSWORD", "admin123")
    suffix = str(time.time_ns())
    username, role_code = "ui_read_" + suffix, "ui_role_" + suffix
    fixture_password = secrets.token_urlsafe(18)
    async with async_playwright() as playwright:
        api = await playwright.request.new_context(base_url=base_url + "/api/")
        browser = await playwright.chromium.launch(headless=True)
        role_id = user_id = None
        try:
            captcha = (await (await api.get("auth/captcha")).json())["data"]
            response = await api.post("auth/login", data={"username": "admin", "password": password,
                "captcha": test_captcha(captcha["uuid"]), "captchaUuid": captcha["uuid"]})
            assert response.ok, "测试管理员登录失败"
            token = (await response.json())["data"]["token"]
            await api.dispose()
            api = await playwright.request.new_context(base_url=base_url + "/api/", extra_http_headers={"Authorization": "Bearer " + token})
            response = await api.post("system/roles", data={"name": "浏览器只读回归", "code": role_code,
                "dataScope": "SELF", "sort": 99, "status": 1, "menuIds": [100, 101]})
            assert response.ok, "测试角色创建失败"
            role_id = (await response.json())["data"]["id"]
            response = await api.post("system/users", data={"username": username, "password": fixture_password,
                "nickname": "浏览器只读回归", "deptId": 5, "status": 1, "roleIds": [role_id]})
            assert response.ok, "测试用户创建失败"
            user_id = (await response.json())["data"]["id"]

            page = await browser.new_page()
            errors = []
            unexpected = []
            requests = []
            page.on("pageerror", lambda error: errors.append(str(error)))
            page.on("response", lambda response: unexpected.append((urlparse(response.url).path, response.status))
                if "/api/" in response.url and response.status >= 400 and urlparse(response.url).path != "/api/health" else None)
            page.on("response", lambda response: requests.append((urlparse(response.url).path, response.status)) if "/api/" in response.url else None)
            await page.goto(base_url)

            # 首次菜单请求晚到：在旧账号初始化过程中模拟会话过期。
            routes_started, release_routes = asyncio.Event(), asyncio.Event()
            first_route = True

            async def hold_old_routes(route):
                nonlocal first_route
                if not first_route:
                    await route.continue_()
                    return
                first_route = False
                response = await route.fetch()
                routes_started.set()
                await release_routes.wait()
                await route.fulfill(response=response)

            await page.route("**/api/auth/routes", hold_old_routes)
            await sign_in(page, "admin", password)
            await asyncio.wait_for(routes_started.wait(), 5)

            async def expired(route):
                await route.fulfill(status=401, json={"code": 401, "message": "登录已过期", "data": None})

            await page.route("**/api/health", expired)
            await request_health(page)
            await page.wait_for_url("**/login**")
            await page.unroute("**/api/health")
            await sign_in(page, username, fixture_password, "/system/user")
            release_routes.set()
            await page.wait_for_load_state("networkidle")
            assert await page.locator(".nav-item").all_text_contents() == ["用户管理"], "旧菜单写回或越权菜单可见"
            assert await page.get_by_role("button", name="新增用户", exact=True).count() == 0, "只读用户看到新增按钮"
            assert await page.get_by_role("button", name="编辑", exact=True).count() == 0, "只读用户看到编辑按钮"
            await page.unroute("**/api/auth/routes")
            await page.goto(base_url + "/system/role")
            await expect(page.get_by_text("页面不存在或没有访问权限", exact=True)).to_be_visible()
            await page.get_by_role("button", name="返回首页").click()
            await page.wait_for_url("**/system/user")
            print("PASS: 旧菜单晚到、低权限落点、按钮隐藏与直接访问限制")

            # 旧账号的 401 与慢退出请求均不得影响随后登录的账号。
            pending_401, release_401 = asyncio.Event(), asyncio.Event()
            pending_logout, release_logout = asyncio.Event(), asyncio.Event()

            async def late_401(route):
                pending_401.set()
                await release_401.wait()
                await expired(route)

            async def slow_logout(route):
                pending_logout.set()
                await release_logout.wait()
                await route.continue_()

            await page.route("**/api/health", late_401)
            await request_health(page, wait=False)
            await asyncio.wait_for(pending_401.wait(), 5)
            await page.route("**/api/auth/logout", slow_logout)
            await sign_out(page)
            await asyncio.wait_for(pending_logout.wait(), 5)
            await sign_in(page, "admin", password, "/dashboard")
            release_401.set()
            release_logout.set()
            await page.wait_for_load_state("networkidle")
            assert page.url.endswith("/dashboard"), "旧请求清除了新登录态"
            assert await page.locator(".nav-item").count() == 11, "管理员菜单不完整"
            await page.unroute("**/api/health")
            await page.unroute("**/api/auth/logout")
            await sign_out(page)
            await sign_in(page, "chensy", password, "/no-access")
            await expect(page.get_by_text("暂未分配可用菜单", exact=True)).to_be_visible()
            await page.reload()
            await expect(page.get_by_text("暂未分配可用菜单", exact=True)).to_be_visible()
            assert not errors, "浏览器运行错误：" + str(errors)
            assert not unexpected, "非预期接口失败：" + str(unexpected)
            print("PASS: 慢退出、旧 401、重新登录及无菜单账号刷新")

            # 真实表单回归：两个角色和两个岗位必须完整回显并原样提交。
            await sign_out(page)
            await sign_in(page, "admin", password, "/dashboard")
            response = await api.put("system/users/" + str(user_id), data={"roleIds": [2, role_id], "postIds": [1, 2]})
            assert response.ok, "多角色夹具配置失败"
            await page.goto(base_url + "/system/user?keyword=" + username)
            await page.get_by_role("button", name="编辑", exact=True).click()
            dialog = page.get_by_role("dialog", name="编辑用户")
            await expect(dialog.get_by_role("group", name="角色").locator("input:checked")).to_have_count(2)
            await expect(dialog.get_by_role("group", name="岗位").locator("input:checked")).to_have_count(2)
            screenshots = Path(__file__).resolve().parents[2] / ".tmp-verification"
            screenshots.mkdir(exist_ok=True)
            await expect(dialog).to_have_css("opacity", "1")
            await expect(dialog.locator("..")).to_have_css("opacity", "1")
            await page.screenshot(path=str(screenshots / "用户多角色编辑.png"), full_page=True)
            await dialog.get_by_placeholder("真实姓名", exact=True).fill("多角色回归已编辑")
            async with page.expect_request(lambda request: request.method == "PUT" and request.url.endswith("/api/system/users/" + str(user_id))) as saved:
                await dialog.get_by_role("button", name="保存", exact=True).click()
            payload = (await saved.value).post_data_json
            assert set(payload["roleIds"]) == {2, role_id}, "编辑提交丢失角色"
            assert set(payload["postIds"]) == {1, 2}, "编辑提交丢失岗位"
            assert payload["deptId"] == 5, "编辑回显了错误的部门"
            await expect(dialog).to_be_hidden()
            response = await api.get("system/users", params={"keyword": username})
            saved_row = (await response.json())["data"]["rows"][0]
            assert set(saved_row["roleIds"]) == {2, role_id} and set(saved_row["postIds"]) == {1, 2}, "数据库未保留完整关联"
            print("PASS: 多角色、多岗位表单回显和保存，部门编号保持一致")

            # 自助改密成功后应立即回到登录页，新密码可以再次登录。
            await sign_out(page)
            await sign_in(page, username, fixture_password, "/system/user")
            await page.locator(".user-chip").click()
            await page.get_by_role("menuitem", name="修改密码", exact=True).click()
            password_dialog = page.get_by_role("dialog", name="修改密码")
            await password_dialog.get_by_label("当前密码").fill(fixture_password)
            await password_dialog.get_by_label(re.compile(r"^新密码")).fill(fixture_password + "X")
            await password_dialog.get_by_label("确认新密码").fill(fixture_password + "X")
            await password_dialog.get_by_role("button", name="确认修改", exact=True).click()
            await page.wait_for_url("**/login**")
            await sign_in(page, username, fixture_password + "X", "/system/user")
            assert not errors, "浏览器运行错误：" + str(errors)
            print("PASS: 自助改密后立即退出并使用新密码登录")

        except Exception:
            print("最近接口状态：", requests[-14:] if 'requests' in locals() else [])
            raise
        finally:
            if user_id:
                response = await api.delete("system/users/" + str(user_id))
                assert response.ok, "测试用户清理失败"
            if role_id:
                response = await api.delete("system/roles/" + str(role_id))
                assert response.ok, "测试角色清理失败"
            await api.dispose()
            await browser.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:5173")
    args = parser.parse_args()
    asyncio.run(main(args.base_url.rstrip("/")))
