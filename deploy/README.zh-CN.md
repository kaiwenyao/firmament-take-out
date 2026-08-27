# 🚀 Firmament 部署捆绑（Deploy Bundle）

**Language / 语言：** [English](README.md)（current）· **简体中文**

本目录收录 firmament 生产环境使用的**基础设施部署文件**：MySQL 初始化 SQL、Redis 初始化脚本与 docker-compose 编排，镜像生产服务器（Hetzner，`/root/data/docker_data/firmament/`）的实际部署。**表结构与生产逐字节一致**；演示数据在收录前已**脱敏**（真实样式的个人信息全部替换为明显假数据），用于可复现、可审计的部署。

> English: This directory contains the production infrastructure bundle — MySQL init SQL, Redis init scripts, and a docker-compose file, kept identical to what actually runs in production.

## 📁 目录结构

```
deploy/
├── docker-compose.yml        # MySQL 8.4 + Redis 7.2 编排（与生产一致）
├── .env.example              # 环境变量示例（复制为 .env 使用，勿提交真实 .env）
└── db/
    ├── init.sql              # MySQL 首次初始化脚本：11 张表 + 演示数据（mysqldump 快照）
    ├── redis-entrypoint.sh   # Redis 自定义入口：首次启动时执行初始化
    └── redis-init.sh         # Redis 初始化：写入 SHOP_STATUS=0（店铺营业状态）
```

## 🚀 快速开始

```bash
# 1. 准备环境变量
cp .env.example .env
# 编辑 .env，设置强密码

# 2. 创建后端容器共用的外部网络（生产同名网络；已存在则跳过）
docker network create firmament_app-network

# 3. 启动
docker compose up -d
```

启动后：

| 组件 | 地址 | 说明 |
|---|---|---|
| MySQL | `firmament-mysql:3306`（容器网络内） | 未暴露宿主机端口，仅限 `firmament_app-network` 内访问 |
| Redis | `127.0.0.1:6379` | 仅绑定本机回环地址 |

后端镜像（`kaiwenyao/firmament-server` 等）加入同一网络后，按 `application-prod.yml` 的配置以 host `firmament-mysql` / `firmament-redis` 连接，数据库账号为 root，密码即 `.env` 中的 `MYSQL_ROOT_PASSWORD`。

## 🗄️ init.sql 的行为与内容

- MySQL 官方镜像约定：`/docker-entrypoint-initdb.d/` 下的脚本**只在数据卷为空的首次启动时执行**（本文件以 `001-init.sql` 挂载）。之后重启、改文件都不会再触发。
- 文件本体是 `mysqldump` 快照（表结构与演示数据取自 2026-04-12 的旧环境），包含：
  - `firmament_take_out` 库全部 **11 张表**的 `DROP TABLE IF EXISTS` + `CREATE TABLE`（即生产 DDL 真身）；
  - 全部表的**英文版演示数据**（菜品、套餐、员工账号、少量测试订单等）。
- 发布前，所有看似真实的个人信息已替换为明显假数据（见下方数据声明）；GTID 导出状态已剔除，可在任意实例干净导入。

### ⚠️ 数据声明

所有看似真实的个人信息在发布前已**清洗为明显假数据**：

| 字段 | 清洗后 |
|---|---|
| 手机号 | `13800000001` … `13800000006`（演示专用号段） |
| 身份证号 | `110101199001010001` … `110101199001010004`（校验位不合法，仅演示） |
| 姓名/用户名 | `Demo Employee`、`demo_staff` 等（仅保留 `admin`、`Rebecca Raynor` 等通用人设） |
| 微信 openid | `ofake00000000000000000000000001` |
| 地址 | `Demo Province / Demo City / Demo Street No. 1` |
| 密码哈希 | 演示账号统一密码 `123456`（bcrypt；两个历史账号保留教程原始 MD5） |

**请勿将真实用户数据提交到本仓库。** 未脱敏的生产快照仅存放在部署服务器本地。

## ♻️ 重建与维护

```bash
# 完全重建（⚠️ 清空数据卷，回到 init.sql 快照状态）
docker compose down -v
docker compose up -d
```

- **修改 `init.sql` 不会影响已在运行的容器**（initdb.d 仅空卷首启执行）；改表结构目前需 `docker exec` 进容器手工 `ALTER`。
- 生产 schema 尚未做版本管理（无 Flyway/Liquibase），本文件是当前唯一的建库真源。规划中的改进：将 `init.sql` 改造为 Flyway `V1__baseline.sql`，后续变更以 `V2__xxx.sql` 增量演进，由 CI 自动验证。
- `db/redis-entrypoint.sh` / `redis-init.sh`：仅 Redis 数据目录为空时执行一次（写入 `SHOP_STATUS=0`），逻辑与 MySQL 侧对称。