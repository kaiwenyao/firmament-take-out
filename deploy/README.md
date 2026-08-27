# 🚀 Firmament Deploy Bundle

**Language / 语言:** **English** (current) · [简体中文](README.zh-CN.md)

This directory versions the **production infrastructure files** for firmament: the MySQL init SQL, the Redis init scripts, and the docker-compose orchestration, mirroring what actually runs on the production server (Hetzner, `/root/data/docker_data/firmament/`). The **schema is byte-faithful to production**; the demo data was **sanitized** (personal-looking fields replaced with obviously fake values) before publishing, making deployments reproducible and auditable.

## 📁 Layout

```
deploy/
├── docker-compose.yml        # MySQL 8.4 + Redis 7.2 orchestration (identical to production)
├── .env.example              # Environment variable template (copy to .env; never commit the real one)
└── db/
    ├── init.sql              # MySQL first-boot init: 11 tables + demo data (mysqldump snapshot)
    ├── redis-entrypoint.sh   # Custom Redis entrypoint: runs init on first start only
    └── redis-init.sh         # Redis init: sets SHOP_STATUS=0 (shop business status)
```

## 🚀 Quick Start

```bash
# 1. Prepare environment variables
cp .env.example .env
# Edit .env and set a strong password

# 2. Create the shared external network used by the backend containers
#    (same name as production; skip if it already exists)
docker network create firmament_app-network

# 3. Start
docker compose up -d
```

Once started:

| Component | Address | Notes |
|---|---|---|
| MySQL | `firmament-mysql:3306` (inside the Docker network) | No host port published; reachable only within `firmament_app-network` |
| Redis | `127.0.0.1:6379` | Bound to loopback only |

Backend images (`kaiwenyao/firmament-server` etc.) join the same network and connect per `application-prod.yml`: host `firmament-mysql` / `firmament-redis`, user `root`, password = `MYSQL_ROOT_PASSWORD` from `.env`.

## 🗄️ What init.sql Does

- MySQL official image convention: scripts mounted under `/docker-entrypoint-initdb.d/` run **only on first startup, when the data volume is empty** (this file is mounted as `001-init.sql`). Restarts and later edits never re-trigger it.
- The file is a `mysqldump` snapshot (schema and demo data taken 2026-04-12 from the previous environment) containing:
  - `DROP TABLE IF EXISTS` + `CREATE TABLE` for all **11 tables** of the `firmament_take_out` schema (the actual production DDL);
  - **English demo data** for every table (dishes, setmeals, employee accounts, a few test orders, etc.).
- Before publishing, every personal-looking field was replaced with obviously fake demo values (see the data notice below). The GTID export state was dropped, so the file imports cleanly anywhere.

### ⚠️ Data Notice

All personal-looking fields were **scrubbed to obviously fake demo values** before publishing:

| Field | Scrubbed to |
|---|---|
| Phone numbers | `138-0000-0001` … `138-0000-0006` (reserved demo range) |
| ID card numbers | `110101199001010001` … `110101199001010004` (invalid checksums, demo-only) |
| Names / usernames | `Demo Employee`, `demo_staff`, … (only generic personas like `admin`, `Rebecca Raynor` kept) |
| WeChat openid | `ofake00000000000000000000000001` |
| Addresses | `Demo Province / Demo City / Demo Street No. 1` |
| Password hashes | All demo accounts use password `123456` (bcrypt; two legacy accounts keep the tutorial's MD5 seed) |

**Never commit real user data to this repository.** The unsanitized production snapshot lives only on the deploy server.

## ♻️ Rebuild & Maintenance

```bash
# Full rebuild (⚠️ wipes the volumes, back to the init.sql snapshot state)
docker compose down -v
docker compose up -d
```

- **Editing `init.sql` does not affect running containers** (initdb.d only runs on an empty volume); schema changes currently require a manual `ALTER` via `docker exec`.
- The production schema has no version management yet (no Flyway/Liquibase); this file is currently the single source of truth for the schema. Planned improvement: convert `init.sql` into a Flyway `V1__baseline.sql` and evolve via incremental `V2__xxx.sql` scripts, verified automatically by CI.
- `db/redis-entrypoint.sh` / `redis-init.sh`: run only once, when the Redis data directory is empty (sets `SHOP_STATUS=0`), mirroring the MySQL-side behavior.