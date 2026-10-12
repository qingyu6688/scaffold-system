# 通用后台管理脚手架

## 项目介绍

本项目是一套基于 **Spring Boot + Vue 3 的通用后台管理脚手架**，面向企业管理系统、内部运营平台等场景，为后续项目提供可复用的基础能力。项目围绕统一组件、统一交互和统一权限体系组织代码，减少新项目在登录认证、组织管理、列表表单等方面的重复开发。

项目采用前后端分离架构。后端使用 Java 17、Spring Boot 3.5、MyBatis、MySQL、Redis 和 JWT，通过 Maven 多模块划分公共基础、框架能力、系统管理、代码生成及启动入口；前端使用 Vue 3、TypeScript、Vite 和 Vue Router，配合自有组件库与 GSAP 实现统一界面和交互。

### 主要功能

- **认证与权限**：验证码登录、会话管理、菜单与按钮权限，以及用户管理中的数据范围和授权校验。
- **系统管理**：用户、角色、菜单、部门、岗位、字典和参数管理，提供查询、分页、编辑、状态管理与导出能力。
- **工作台与日志**：展示登录趋势、操作统计、最近登录记录，支持操作日志、登录日志查询及异常分析。
- **可视化管理**：提供用户看板、组织架构画布、角色权限矩阵、权限关系图和菜单导航预览，便于理解组织与授权关系。
- **个人中心**：支持查看个人信息、角色岗位及本人登录记录，维护姓名和联系方式。
- **代码生成**：支持数据库表导入、字段信息查看、代码预览和 ZIP 下载，生成结果作为后续业务开发的骨架，业务逻辑与页面需结合具体项目完善。

### 界面与开发方式

界面采用统一的“珍珠玻璃”设计语言，以清晰的阅读层级、克制的浮层和一致的控件样式组织信息，支持浅色、深色及跟随系统外观。列表、筛选、表单、弹窗、权限按钮和组织选择等能力均沉淀为公共组件，方便新增模块沿用已有规范。

当前项目定位为**本地开发底座**。后续业务可在现有认证、权限、组织和组件体系上扩展，重点实现自身的数据结构、业务规则与操作流程。

## 开发入口

- [项目地图](PROJECT_MAP.md)：按功能定位页面、接口、服务、SQL 和权限。
- [设计规范](DESIGN.md)：珍珠玻璃主题、组件复用及交互规范。
- [开发约定](AGENTS.md)、[更新日志](changelog.md)。
- [环境说明](docs/环境说明.md)、[本地数据库接入说明](docs/本地数据库接入说明.md)、[安全修复说明](docs/安全修复说明.md)、[测试报告](docs/测试报告.md)。
- [后端架构](docs/backend-architecture.md)、[前后端接口约定](docs/frontend-backend-integration.md)、[前端架构](docs/admin-redesign-architecture.md)。

## 目录与技术栈

| 目录 | 用途 | 技术栈 |
| --- | --- | --- |
| `scaffold-system` | 后端 Maven 多模块工程 | Java 17+、Spring Boot 3.5、MyBatis、MySQL、Redis、JWT |
| `scaffold-vue` | 管理后台 | Vue 3、TypeScript、Vite 6、Vue Router 4、GSAP 3，自有组件库 |
| `scaffold-system/sql` | 首次初始化脚本 | `V1__init_schema.sql` 建表，`V2__seed_data.sql` 初始化数据，`V3__user_session_version.sql` 增加会话版本 |
| `docs` | 当前架构说明 | 后端架构、前后端接口约定和前端组件架构 |

当前包含工作台、10 个管理页面和个人中心；建筑、工作流模块已移除。

## 本地启动

准备 JDK 17 或更新版本、Maven、Node.js 22.13+ 或 24+、MySQL（已验证8.0）和 Redis 6.2+；前后端依赖均使用项目现有配置。

Windows 本地启动可使用 `scaffold-system/Start-Local.ps1`，它会读取同目录下不纳入 Git 的 `.env`。首次配置参考 `.env.example`；不要覆盖已经填写好的本机配置。先停止占用后端端口的旧进程，然后在项目根目录执行：

```powershell
.\scaffold-system\Start-Local.ps1 -Build
```

已有最新构建时可省略 `-Build`；`-ValidateOnly` 只检查配置格式，不连接服务。当前机器的数据库导入与连接记录见[本地数据库接入说明](docs/本地数据库接入说明.md)。

直接运行 Java 或 IDE 时，后端读取进程环境变量，不会自行加载 `.env` 文件；需在 IDE 运行配置或启动终端中设置：

| 变量 | 中文说明 | 默认值 |
| --- | --- | --- |
| `JWT_SECRET` | JWT 签名密钥，至少 32 字节且不能全为空白 | 必填，无默认值 |
| `DB_HOST` / `DB_PORT` | MySQL 地址 / 端口 | `localhost` / `3306` |
| `DB_NAME` | 数据库名称 | `scaffold_system` |
| `DB_USERNAME` / `DB_PASSWORD` | 数据库账号 / 密码 | 本地开发默认账号；按实际环境配置 |
| `DB_TIMEZONE` | 数据库连接与会话时区 | `+08:00` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 地址 / 端口 | `localhost` / `6379` |
| `REDIS_PASSWORD` | Redis 密码 | 空；按实际环境配置 |
| `REDIS_DATABASE` | Redis 逻辑库编号，共享实例时选择独立库 | `0` |
| `SERVER_PORT` | 后端 HTTP 端口 | `8080` |
| `VITE_API_PROXY_TARGET` | 前端开发代理的后端地址 | `http://localhost:8080` |

可在启动后端的终端中生成本机密钥；生成结果保留在当前进程环境，不写入仓库：

```bash
export JWT_SECRET="$(openssl rand -hex 32)"
```

使用 IDE 时将密钥配置到运行环境。未配置或不足 32 字节时后端启动失败；保留同一密钥可让重启前的有效 token 继续验签，更换密钥会使旧 token 失效。

首次创建空的 `scaffold_system` 库后，按 V1 → V2 → V3 顺序导入三份 SQL；mysql 客户端须使用 `--default-character-set=utf8mb4`，并通过 `--init-command="SET time_zone='+08:00'"` 设置会话时区。也可在 IDE 运行 `scaffold-admin` 中的 `com.scaffold.admin.util.DbInit`，该工具使用同一组 `DB_*` 变量并按版本顺序初始化；参数 `fix` 会删除整个目标库，仅在核实库名、备份且明确同意重建时使用。

```bash
# 在 scaffold-system 目录构建与启动后端
mvn -pl scaffold-admin -am package -DskipTests
java -jar scaffold-admin/target/scaffold-admin-0.1.3-SNAPSHOT.jar
```

```bash
# 在 scaffold-vue 目录启动前端；首次使用先 npm ci
npm run dev
```

浏览器访问 `http://localhost:5173`，后端本地端口为 `8080`；前端统一通过 `/api` 代理访问后端。

种子库管理员为 `admin`，开发密码为 `admin123`；仅用于本地开发，接入实际环境前请修改密码并配置自己的数据库与 Redis 凭据。种子组织和用户为示例，电话留空，邮箱使用 `example.com`。验证码填写页面图片中的字符；接口只返回图片和标识。新建用户、重置密码时必须明确输入密码。超级管理员可见全部启用且可见的已注册菜单，并拥有全部操作权限；其余种子角色默认未授予菜单，需在角色管理中分配。无可用菜单的账号登录后显示“暂未分配可用菜单”。

旧库升级仅执行一次 V3，详细步骤见[安全修复说明](docs/安全修复说明.md)。用户后台编辑、停用或改密后旧会话失效；管理员在用户管理中编辑自己的账号时保留当前会话并同步刷新顶栏、权限与菜单，把自己停用仍立即退出。角色、菜单和部门结构写操作仅允许超级管理员；普通管理员的用户写操作受数据范围与授予范围限制。

## 新业务模块

1. 先设计业务表与中文字段含义；默认包含 `create_by`、`update_by`、`create_time`、`update_time` 四个审计字段。
2. 复用 `scaffold-common` 的返回、分页与异常，以及 `scaffold-framework` 的权限、数据权限、操作日志能力。
3. 前端列表复用 `ListPage`、`useListPage` / `useTreeListPage`；表单复用 `FormDialog`、`Field` 和现有控件，操作复用 `PermissionButton`。
4. 在 `src/api` 接入统一 HTTP 客户端，在 `src/composables/auth.ts` 的 `COMPONENT_MAP` 注册页面，再添加菜单和按钮权限。
5. 按角色配置菜单和数据范围，验证低权限账号的真实接口拒绝行为、查询、分页、导出及表单校验。
6. 同步项目地图、相关接口说明和更新日志。

代码生成初始配置为空，按需导入当前数据库的真实表，支持字段信息查看、配置说明保存、7 个文件预览和 ZIP 下载。输出为开发骨架：Controller、Service、Mapper 的业务 CRUD 仍需实现，前端页面需按统一组件规范补齐；下载不会自动写入本工程。

## 验证

不连接数据库和 Redis 的测试与编译：

```bash
# 在 scaffold-system 目录
mvn -pl scaffold-system -am test
mvn -pl scaffold-admin -am package -DskipTests
```

完整后端集成测试需要配置上述环境变量并准备独立校验库：

```bash
# 在 scaffold-system 目录；建议用独立校验库，测试会写入夹具和日志
mvn -pl scaffold-admin -am test
```

```bash
# 在 scaffold-vue 目录
npm run lint
npm test
npm run build
npm audit --registry=https://registry.npmjs.org
# 已安装 Python Playwright 与 Chromium 时，可验证真实浏览器会话与权限
SCAFFOLD_TEST_REDIS_PORT=<独立测试Redis端口> python3 scripts/session-regression.py
```

浏览器回归脚本只创建并删除自己生成的测试角色和用户，不清空表；管理员密码可通过 `SCAFFOLD_TEST_PASSWORD` 覆盖。浏览器脚本必须显式指定独立测试 Redis，直接从该测试实例读取答案，不要求生产接口返回验证码字符。测试与构建成功不能代替业务页面验收。

## 许可证

本项目代码采用 [MIT License](LICENSE)，允许商用、修改和分发，使用时保留版权及许可证声明。

第三方依赖遵循各自的许可证。GSAP 使用 [Standard "No Charge" License](https://gsap.com/community/standard-license/)，不属于本项目的 MIT 授权范围；使用与再分发时须遵守其条款并保留原有声明。
