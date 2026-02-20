# IDVerse Quick Start

Get up and running in 5 minutes. Choose your deployment path below.

---

## Prerequisites

- **Java 21** (local development)
- **Maven 3.6+** (local development)
- **Docker + Docker Compose** (production deployment)

> **No separate library build needed.** The `idverse-api` client library is committed to `local-repo/` and resolved automatically by Maven via a `file://` repository URL — no need to clone or build the `idverse-api` repo.

---

## Path 1: Development (Fastest)

**H2 in-memory database** - Data resets on restart. Perfect for quick testing.

```bash
# 1. Clone and navigate
cd idverse

# 2. Copy environment template
cp .env.example .env

# 3. Run
mvn spring-boot:run
```

**Access:** http://localhost:19746

---

## Path 2: Development with Persistence

**H2 file-based database** - Data persists in `./data/idverse.db`

```bash
# 1. Set database type
export DATABASE_TYPE=h2-file

# 2. Run
mvn spring-boot:run
```

**Alternative:** Use Spring profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2-file
```

**Data location:** `./data/idverse.db`
**Access:** http://localhost:19746/h2-console

---

## Path 3: Production (Docker)

**MySQL 8.0** - Persistent database with Docker Compose

```bash
# 1. Copy and edit environment
cp .env.example .env
# Edit .env: Set IDVERSE_CLIENT_ID, IDVERSE_CLIENT_SECRET, AUTH_KEY, JWT_SECRET_KEY

# 2. Start services
docker-compose up -d

# 3. View logs
docker-compose logs -f
```

**Access:** http://localhost:19746
**MySQL Port:** 3307 (to avoid conflicts)

📖 **Full Docker guide:** [DOCKER.md](DOCKER.md)

---

## Database Options Summary

| Method | Command | Data Persistence | Use Case |
|--------|---------|------------------|----------|
| **H2 Memory** (default) | `mvn spring-boot:run` | ❌ No | Quick testing |
| **H2 File** | `DATABASE_TYPE=h2-file mvn spring-boot:run` | ✅ Yes (`./data/`) | Local development |
| **MySQL** (Docker) | `docker-compose up -d` | ✅ Yes (Docker volume) | Production |

### Switching Databases

**Option 1: Environment Variable**
```bash
export DATABASE_TYPE=h2-memory  # Default, in-memory
export DATABASE_TYPE=h2-file    # File-based H2
export DATABASE_TYPE=docker     # MySQL (use with Docker)
```

**Option 2: Spring Profile** (overrides env var)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2-file
```

---

## Build Commands

```bash
# Build
mvn clean install

# Run tests
mvn test

# Package
mvn package

# Skip tests
mvn clean install -DskipTests
```

📖 **Full build guide:** [CLAUDE.md](CLAUDE.md#build-commands)

---

## Configuration

### Required Environment Variables

Edit `.env` file:

```bash
# IDVerse API (Required)
IDVERSE_CLIENT_ID=your_client_id
IDVERSE_CLIENT_SECRET=your_client_secret

# Authentication (Required)
AUTH_KEY=your_auth_key
JWT_SECRET_KEY=your_jwt_secret
```

### Optional Configuration

```bash
# API Endpoints
IDVERSE_OAUTH_URL=https://us.demo.idkit.co/api/3.5/oauthToken
IDVERSE_API_URL=https://us.demo.idkit.co/api/3.5/sendSms

# Logging (DEBUG, INFO, SECRET)
VERBOSE=DEBUG

# Webhooks
NOTIFY_URL_COMPLETE=http://localhost:19746/api/webhook
NOTIFY_URL_EVENT=http://localhost:19746/api/webhook
```

📖 **Full configuration guide:** [README.md](README.md#configuration)

---

## Verification

Test your setup:

```bash
# Check OAuth configuration
curl http://localhost:19746/test/oauth

# Check health
curl http://localhost:19746/test/config
```

Expected response:
```json
{"status":"SUCCESS","message":"Configuration is valid..."}
```

## Key API Endpoints

### Submit a verification
```bash
curl -X POST http://localhost:19746/api/verify \
  -H "Content-Type: application/json" \
  -d '{"phoneCode": "+1", "phoneNumber": "9412607454", "referenceId": "REF123"}'
```

### Test without calling the real API (dry run)
```bash
curl -X POST "http://localhost:19746/api/verify/test?dryRun=true" \
  -H "Content-Type: application/json" \
  -d '{"phoneCode": "+1", "phoneNumber": "9412607454", "referenceId": "REF123"}'
```
Saves the record with a mock response — no SMS sent. Use `dryRun=false` (or omit) to make the real call.

### Update status (no JWT required)
```bash
curl -X POST http://localhost:19746/api/updateStatus \
  -H "Content-Type: application/json" \
  -d '{"transactionId": "txn-example-0000001", "event": "completedPass"}'
```
Use `event` (camelCase IDVerse name) or `status` (explicit uppercase string), or both — `event` takes precedence.

### Check latest status
```bash
# By reference ID
curl http://localhost:19746/api/status/reference/REF123

# By transaction ID
curl http://localhost:19746/api/status/transaction/txn-1234567890-abc123
```

**Status response:**
```json
{"status": "SMS SENT", "timestamp": "2026-02-18T12:00:00", "errorMessage": null}
```

Returns **404** if no record is found for the given ID.

📖 **Full API reference:** [README.md](README.md#api-endpoints)

---

## Next Steps

- 📖 **Full Documentation:** [README.md](README.md)
- 🐳 **Docker Deployment:** [DOCKER.md](DOCKER.md)
- 🔧 **Development Guide:** [CLAUDE.md](CLAUDE.md)
- 🔌 **API Endpoints:** [README.md](README.md#api-endpoints)

---

## Troubleshooting

**Port 19746 already in use:**
```bash
# Change port in application.yml or:
export SERVER_PORT=8080
mvn spring-boot:run
```

**Database connection errors (Docker):**
```bash
# Check services
docker-compose ps

# View logs
docker-compose logs mysql
```

**OAuth token errors:**
```bash
# Verify credentials in .env
cat .env | grep IDVERSE_CLIENT

# Clear token cache
curl -X POST http://localhost:19746/test/oauth/clear
```

📖 **Full troubleshooting:** [README.md](README.md#troubleshooting)
