# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**IDVerse Verification Service** - A Spring Boot 3.2.1 web application that integrates with the IDVerse API for phone number verification.

- **GroupId:** `org.itnaf`
- **ArtifactId:** `idverse`
- **Java Version:** 21
- **Build Tool:** Maven
- **Framework:** Spring Boot 3.2.1
- **Port:** 19746

### Multi-Repo Structure

This project is split across two repositories:

| Repo | Path | Purpose |
|------|------|---------|
| `idverse` | `/Users/mfanti/Documents/BehavioSec/IDVerse/idverse` | Web application (this repo) |
| `idverse-api` | `/Users/mfanti/Documents/BehavioSec/IDVerse/idverse-api` | API client library source |

The `idverse-api` library is bundled as a pre-built JAR committed to this repo. No separate build step is required — Maven resolves it via a local file-system repository (`local-repo/`) referenced in `pom.xml` with a `file://` URL. This works in any environment (local, Docker, GCP Cloud Build) that has the repo checked out.

## Technology Stack

### Core Dependencies
- **Spring Boot Starter Web** - REST API and web UI
- **Spring Boot Starter Data JPA** - Database persistence
- **Spring Boot Starter Thymeleaf** - Template engine for web UI
- **Spring Boot Starter WebFlux** - WebClient for external API calls (via idverse-api)
- **Spring Boot Starter Validation** - Request validation
- **idverse-api (1.0-SNAPSHOT)** - IDVerse API client library (resolved from `local-repo/` via `file://` Maven repository)

### Database Support
- **H2 Database** - In-memory database (default/development profile)
- **MySQL Connector** - Production database (docker profile)

### Security & Authentication
- **JWT (jjwt 0.12.3)** - JSON Web Token implementation for webhook authentication (via idverse-api)
- Custom session-based authentication with AUTH_KEY

### Utilities
- **Dotenv Java (3.0.0)** - Environment variable management from `.env` file
- **Lombok** - Boilerplate code reduction

## Build Commands

```bash
# Build the project
mvn clean install

# Compile only
mvn compile

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a single test method
mvn test -Dtest=ClassName#methodName

# Package the application
mvn package

# Skip tests during build
mvn clean install -DskipTests

# Run the application locally
mvn spring-boot:run
```

## Docker Deployment

### Docker Architecture

The application supports containerized deployment with Docker Compose:

**Components:**
1. **Application Container** - Spring Boot app running on Eclipse Temurin 21 JRE Alpine
2. **MySQL Container** - MySQL 8.0 with persistent storage
3. **Network** - Dedicated bridge network (`idverse-network`) for service isolation
4. **Volume** - Named volume (`mysql_data`) for permanent database storage

**Design Decisions:**
- **Multi-stage Docker build** - Reduces final image size by separating build and runtime stages
- **Non-root user** - Security best practice, app runs as `spring:spring` user
- **Health checks** - MySQL container has health check; app waits for healthy MySQL before starting
- **Spring profiles** - Automatic profile switching (default = H2, docker = MySQL)
- **Environment-based configuration** - All sensitive values externalized to `.env` file

### Docker Files

- **`Dockerfile`** - Multi-stage build (builder + runtime)
- **`docker-compose.yml`** - Service orchestration with MySQL and app
- **`application-docker.yml`** - Spring profile for MySQL configuration
- **`.dockerignore`** - Build context optimization
- **`.env.example`** - Template for environment variables
- **`DOCKER.md`** - Complete deployment guide

### Docker Commands

```bash
# Start services (detached)
docker-compose up -d

# View logs
docker-compose logs -f app

# Rebuild after code changes
docker-compose up -d --build app

# Stop services (preserve data)
docker-compose down

# Stop and remove volumes (DELETE DATA)
docker-compose down -v

# Access MySQL
docker-compose exec mysql mysql -u idverse_user -p

# Backup database
docker-compose exec mysql mysqldump -u root -p idverse > backup.sql
```

### Environment Variables

The `.env` file configures both application and Docker deployment:

**IDVerse API Configuration:**
- `IDVERSE_CLIENT_ID` - OAuth client ID
- `IDVERSE_CLIENT_SECRET` - OAuth client secret
- `IDVERSE_OAUTH_URL` - OAuth token endpoint
- `IDVERSE_API_URL` - SMS verification endpoint

**Webhook Configuration:**
- `NOTIFY_URL_COMPLETE` - Webhook callback URL for completion
- `NOTIFY_URL_EVENT` - Webhook callback URL for events
- `JWT_SECRET_KEY` - Secret for JWT webhook authentication

**Authentication:**
- `AUTH_KEY` - Web interface authentication key

**Database Configuration:**
- `DATABASE_TYPE` - Database selection: `h2-memory` (default), `h2-file`, `docker`
  - Note: Spring profile `--spring.profiles.active` overrides this
  - Example: `export DATABASE_TYPE=h2-file`

**Database (MySQL/Docker):**
- `MYSQL_ROOT_PASSWORD` - MySQL root password
- `MYSQL_DATABASE` - Database name (default: idverse)
- `MYSQL_USER` - Application database user
- `MYSQL_PASSWORD` - Application database password
- `MYSQL_PORT` - MySQL port (default: 3307 to avoid conflicts)

**Application Settings:**
- `VERBOSE` - Logging level (DEBUG, INFO, SECRET)
- `TRANSACTION` - Default transaction ID
- `NAME` - Default name for forms
- `DDL_AUTO` - Hibernate DDL mode (update, validate, create, create-drop)
- `SHOW_SQL` - Show SQL queries in logs (true/false)

## Project Structure

### This repo (`idverse`) — Web Application

```
idverse/
├── src/main/java/org/itnaf/idverse/
│   ├── IdverseApplication.java          # Main Spring Boot application
│   ├── config/
│   │   └── ApiConfig.java               # Config beans (loads .env, provides
│   │                                    #   String beans consumed by idverse-api)
│   ├── controller/
│   │   ├── ApiController.java           # REST API endpoints
│   │   ├── WebController.java           # Web UI controller
│   │   ├── OAuthTestController.java     # OAuth testing endpoints
│   │   ├── WebhookController.java       # Webhook receiver
│   │   ├── MockOAuthController.java     # Mock OAuth endpoint
│   │   └── AuthController.java         # Auth key endpoint
│   ├── service/
│   │   ├── IdVerificationService.java   # Orchestrates API call + DB persistence
│   │   ├── AuthService.java            # Session key management
│   │   └── SessionService.java         # Session management
│   ├── model/
│   │   ├── VerificationResponse.java   # Response DTO (includes DB fields)
│   │   └── VerificationRecord.java     # JPA Entity
│   └── repository/
│       └── VerificationRepository.java  # Spring Data JPA repository
├── src/main/resources/
│   ├── application.yml                  # Default configuration (H2)
│   ├── application-docker.yml           # Docker profile (MySQL)
│   ├── templates/                       # Thymeleaf templates
│   └── static/                          # Static assets
├── src/test/java/                       # Test sources
├── docker/                              # Docker-related files
│   └── mysql/
│       ├── conf.d/                      # Custom MySQL config
│       └── init/                        # DB initialization scripts
├── pom.xml                              # Maven configuration
├── Dockerfile                           # Application container image
├── docker-compose.yml                   # Service orchestration
├── lib/
│   └── idverse-api-1.0-SNAPSHOT.jar    # Source JAR (copy here when updating)
├── local-repo/                          # Local Maven repository (committed)
│   └── org/itnaf/idverse-api/          # Resolved by Maven via file:// URL
├── .env                                 # Environment variables (gitignored)
├── .env.example                         # Environment template
├── data/                                # H2 file database storage (gitignored)
├── QUICKSTART.md                        # Quick start guide
├── CLAUDE.md                            # This file
├── DOCKER.md                            # Docker deployment guide
└── README.md                            # Project documentation
```

### Sibling repo (`idverse-api`) — API Client Library

```
idverse-api/
└── src/main/java/org/itnaf/idverse/client/
    ├── IdVerseApiClient.java            # HTTP client for IDVerse API
    ├── model/
    │   ├── VerificationRequest.java     # API request DTO
    │   ├── OAuthTokenResponse.java      # OAuth token response DTO
    │   └── WebhookPayload.java          # Incoming webhook payload DTO
    └── service/
        ├── OAuthTokenService.java       # OAuth token management (caching)
        └── JwtService.java              # JWT generation/validation
```

**Package:** `org.itnaf.idverse.client` — a sub-package of the app's base package, so Spring Boot's component scan picks up `@Service` beans from the library automatically without any extra configuration.

**Dependency wiring:** The library's services are injected with String beans (`idverseClientId`, `idverseOAuthUrl`, etc.) that are defined as `@Bean` methods in the app's `ApiConfig.java`.

## Development Environment

- **Java Version:** 21
- **Build Tool:** Maven 3.6+
- **IDE:** IntelliJ IDEA (configuration files present in `.idea/`)
- **Local Database:** H2 in-memory (default)
- **Production Database:** MySQL 8.0 (Docker)

## Database Configuration Strategy

### Profile Hierarchy

The application uses a dual-configuration approach for maximum flexibility:

1. **Environment Variable:** `DATABASE_TYPE` (default: `h2-memory`)
2. **Spring Profile:** `--spring.profiles.active` (overrides env var)

**Priority:** Spring Profile > Environment Variable > Default

### Available Database Configurations

| Profile | Environment | Database | Persistence | Use Case |
|---------|-------------|----------|-------------|----------|
| `default` | `DATABASE_TYPE=h2-memory` | H2 in-memory | ❌ No | Quick development/testing |
| `h2-file` | `DATABASE_TYPE=h2-file` | H2 file-based | ✅ Yes (`./data/`) | Local development |
| `docker` | `DATABASE_TYPE=docker` | MySQL 8.0 | ✅ Yes (volume) | Production deployment |

### Profile Details

#### Default Profile (H2 In-Memory)
- **Driver:** H2 Database
- **URL:** `jdbc:h2:mem:testdb`
- **Console:** http://localhost:19746/h2-console
- **Credentials:** sa / sa
- **Auto-schema:** `update`
- **Persistence:** No (data lost on restart)
- **Best for:** Quick testing, CI/CD, ephemeral environments

#### H2 File Profile (H2 Persistent)
- **Driver:** H2 Database
- **URL:** `jdbc:h2:file:./data/idverse`
- **Console:** http://localhost:19746/h2-console
- **Credentials:** sa / sa
- **Auto-schema:** `update`
- **Persistence:** Yes (file: `./data/idverse.mv.db`)
- **Best for:** Local development with data retention

**Activation:**
```bash
# Via environment variable
export DATABASE_TYPE=h2-file
mvn spring-boot:run

# Via Spring profile (overrides env var)
mvn spring-boot:run -Dspring-boot.run.profiles=h2-file
```

#### Docker Profile (MySQL Production)
- **Driver:** MySQL 8.0
- **URL:** `jdbc:mysql://mysql:3306/idverse`
- **Credentials:** Configured via environment variables
- **Auto-schema:** `update` (configurable via DDL_AUTO env var)
- **Persistence:** Permanent via Docker volume `mysql_data`
- **Connection Pool:** HikariCP (max 10 connections, min 5 idle)
- **Best for:** Production, staging, Docker deployments

**Activation:**
```bash
# Automatically activated in Docker Compose via SPRING_PROFILES_ACTIVE=docker
docker-compose up -d
```

## Architectural Decisions

### API Client Library Separation

**Decision:** Extract IDVerse HTTP client code into a separate `idverse-api` Maven library.

**Repo:** https://github.com/marcofanti/idverse-api

**What's in the library (`org.itnaf.idverse.client`):**
- `IdVerseApiClient` — single entry point; sends the SMS verification HTTP POST
- `OAuthTokenService` — OAuth token fetching with 800-second cache
- `JwtService` — JWT generation/validation for webhook auth
- `model/VerificationRequest` — API request DTO
- `model/OAuthTokenResponse` — OAuth response DTO
- `model/WebhookPayload` — incoming webhook payload DTO

**What stays in this app:**
- `IdVerificationService` — calls `IdVerseApiClient`, persists result to DB
- `VerificationRecord` — JPA entity (DB-specific)
- `VerificationResponse` — response DTO including DB-generated `id` and `timestamp`
- All controllers, config, Thymeleaf templates

**Why sub-package (`org.itnaf.idverse.client`):**  Spring Boot scans `org.itnaf.idverse` and all sub-packages, so `@Service` beans in the library are discovered automatically. No auto-configuration file or `@ComponentScan` change needed in this app.

**How the JAR is resolved:** The JAR lives in `local-repo/` (a local Maven repository committed to this repo) and is declared in `pom.xml` as a standard compile-scope dependency. A `<repository>` entry with `file://${project.basedir}/local-repo` tells Maven where to find it — no external registry, no `mvn install` to `~/.m2` required. This works identically in local dev, Docker, and GCP Cloud Build.

**Updating the library JAR:**

If the `idverse-api` source is changed and a new JAR is produced:
```bash
# 1. Copy the new JAR into lib/
cp /path/to/idverse-api-1.0-SNAPSHOT.jar lib/

# 2. Re-install into the local Maven repository
mvn install:install-file \
  -Dfile=lib/idverse-api-1.0-SNAPSHOT.jar \
  -DgroupId=org.itnaf -DartifactId=idverse-api \
  -Dversion=1.0-SNAPSHOT -Dpackaging=jar \
  -DlocalRepositoryPath=./local-repo

# 3. Commit both lib/ and local-repo/
git add lib/ local-repo/
git commit -m "Update idverse-api library JAR"
```

### Database Configuration Design

**Decision:** Dual-configuration approach (Environment Variable + Spring Profiles)

**Rationale:**
- **Environment Variable (`DATABASE_TYPE`)** - Simple, works across all environments, no command-line flags needed
- **Spring Profile Override** - Power users can explicitly control database selection
- **Hierarchy** - Clear precedence (Profile > Env Var > Default) prevents ambiguity

**Benefits:**
1. **Developer Flexibility** - Quick switching without editing config files
2. **CI/CD Friendly** - Environment variables integrate easily with pipelines
3. **Docker Native** - Profiles automatically activated in containerized environments
4. **Explicit Control** - Profiles override when precision matters
5. **Safe Defaults** - H2 in-memory prevents accidental data persistence in tests

**Implementation:**
```yaml
# application.yml
spring:
  profiles:
    active: ${DATABASE_TYPE:default}
```

**Files:**
- `application.yml` - Default profile (H2 in-memory) + env var activation
- `application-h2-file.yml` - File-based H2 configuration
- `application-docker.yml` - MySQL configuration for Docker

### H2 File Storage Location

**Decision:** Store H2 database files in `./data/` directory

**Rationale:**
- **Visibility** - Easy to find and backup
- **Gitignored** - Won't be accidentally committed
- **Project-scoped** - Keeps data with the project
- **Easy cleanup** - `rm -rf data/` to reset

**File Structure:**
```
data/
├── idverse.mv.db          # H2 database file
└── idverse.trace.db       # H2 trace file (if enabled)
```

## Key Features & Architectural Decisions

### OAuth Token Caching
- Tokens cached for 800 seconds (13.3 minutes)
- Reduces API load and improves response times
- Thread-safe implementation
- Manual cache clearing available via `/test/oauth/clear`

### Webhook Authentication
- JWT-based authentication using `JWT_SECRET_KEY`
- Validates incoming webhook requests from IDVerse
- Prevents unauthorized webhook submissions

### Session Management
- Custom session-based authentication
- AUTH_KEY required for web interface access
- Sessions stored in-memory (consider Redis for production clustering)

### Error Handling
- HTML response detection prevents error pages from being treated as success
- Comprehensive validation on API requests
- Detailed error messages in development, sanitized in production

### Logging Modes
- **INFO** - Standard logging
- **DEBUG** - Detailed logging with sensitive data masked
- **SECRET** - Complete request/response logging (console only, for debugging)

## Testing Endpoints

### OAuth Testing
- `GET /test/oauth` - Test OAuth token retrieval
- `GET /test/oauth?verbose=debug` - Verbose OAuth test with full details
- `POST /test/oauth/clear` - Clear token cache
- `GET /test/config` - Test configuration

### Mock OAuth
- `POST /api/3.5/oauthToken` - Mock OAuth endpoint (returns OAUTHTOKEN from .env)

## Security Considerations

### Docker Security
- Non-root user in container (`spring:spring`)
- Minimal base image (Alpine Linux)
- No unnecessary packages in runtime image
- Secrets via environment variables (not hardcoded)

### Application Security
- JWT webhook authentication
- Session-based web authentication
- Environment-based credential management
- SQL injection prevention via JPA/Hibernate
- Input validation on all endpoints

### Production Recommendations
1. Change all default passwords in `.env`
2. Use secrets management (e.g., Docker Secrets, Kubernetes Secrets)
3. Set `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` (prevent auto-schema changes)
4. Enable MySQL SSL connections
5. Implement rate limiting on API endpoints
6. Set up regular database backups
7. Use external MySQL (not containerized) for critical production
8. Implement proper monitoring and alerting

## Common Development Tasks

### Switching Databases

**Quick switch via environment variable:**
```bash
# H2 in-memory (default)
mvn spring-boot:run

# H2 file-based (persistent)
export DATABASE_TYPE=h2-file
mvn spring-boot:run

# MySQL (use Docker Compose)
docker-compose up -d
```

**Explicit profile override:**
```bash
# Override environment variable with profile
mvn spring-boot:run -Dspring-boot.run.profiles=h2-file

# Multiple profiles
mvn spring-boot:run -Dspring-boot.run.profiles=h2-file,debug
```

### Accessing H2 Console

**H2 In-Memory:**
- URL: http://localhost:19746/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `sa`

**H2 File-Based:**
- URL: http://localhost:19746/h2-console
- JDBC URL: `jdbc:h2:file:./data/idverse`
- Username: `sa`
- Password: `sa`

### Resetting Database

**H2 In-Memory:** Restart application

**H2 File-Based:**
```bash
rm -rf data/
```

**MySQL (Docker):**
```bash
docker-compose down -v  # WARNING: Deletes all data
docker-compose up -d
```

### Working with the API Client Library

**Library JAR:** `lib/idverse-api-1.0-SNAPSHOT.jar` (committed to this repo)

**Source repo:** `/Users/mfanti/Documents/BehavioSec/IDVerse/idverse-api`

**After modifying the library source:**
```bash
cd ../idverse-api
mvn clean package -DskipTests                                    # builds the JAR
cp target/idverse-api-1.0-SNAPSHOT.jar ../idverse/lib/
cd ../idverse
mvn install:install-file \
  -Dfile=lib/idverse-api-1.0-SNAPSHOT.jar \
  -DgroupId=org.itnaf -DartifactId=idverse-api \
  -Dversion=1.0-SNAPSHOT -Dpackaging=jar \
  -DlocalRepositoryPath=./local-repo
git add lib/ local-repo/
git commit -m "Update idverse-api library JAR"
mvn spring-boot:run                                              # picks up the updated JAR
```

**Adding a field to `VerificationRequest`:**
1. Edit `VerificationRequest.java` in `idverse-api`
2. Update `IdVerseApiClient.sendVerification()` to include the field in the request body
3. Rebuild, copy JAR to `lib/`, re-install into `local-repo/`, and commit both
4. Update callers in this app (controllers, tests) if needed

**Adding a new API operation:**
1. Create a new method in `IdVerseApiClient` (in `idverse-api`)
2. Rebuild, copy JAR to `lib/`, re-install into `local-repo/`, and commit both
3. Call the new method from `IdVerificationService` (or a new service) in this app

### Adding a New Endpoint
1. Create controller method with appropriate mapping
2. Add service layer logic if needed
3. Define DTOs for request/response
4. Add validation annotations
5. Update API documentation in README.md

### Adding a New Environment Variable
1. Add to `.env` file
2. Update `.env.example`
3. Load in `ApiConfig.java` if needed
4. Document in CLAUDE.md and README.md

### Database Schema Changes
1. Modify JPA entity classes
2. Run application (DDL auto-update will apply changes)
3. For production: create migration scripts instead of relying on auto-update

### Creating a New Database Profile
1. Create `application-{profile}.yml` in `src/main/resources/`
2. Configure datasource, JPA, and other settings
3. Document in CLAUDE.md
4. Add to `DATABASE_TYPE` options in `.env.example`
5. Update QUICKSTART.md with usage example

### Troubleshooting

**Application won't start in Docker:**
- Check MySQL health: `docker-compose ps`
- View logs: `docker-compose logs mysql`
- Verify .env file exists and is properly formatted

**Database connection errors:**
- Ensure MySQL is healthy before app starts (health check configured)
- Verify credentials in .env match docker-compose.yml
- Check network connectivity: `docker network inspect idverse-network`

**OAuth token issues:**
- Test credentials: `curl http://localhost:19746/test/oauth`
- Clear cache: `curl -X POST http://localhost:19746/test/oauth/clear`
- Check .env file has valid IDVERSE_CLIENT_ID and IDVERSE_CLIENT_SECRET
