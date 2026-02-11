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

## Technology Stack

### Core Dependencies
- **Spring Boot Starter Web** - REST API and web UI
- **Spring Boot Starter Data JPA** - Database persistence
- **Spring Boot Starter Thymeleaf** - Template engine for web UI
- **Spring Boot Starter WebFlux** - WebClient for external API calls
- **Spring Boot Starter Validation** - Request validation

### Database Support
- **H2 Database** - In-memory database (default/development profile)
- **MySQL Connector** - Production database (docker profile)

### Security & Authentication
- **JWT (jjwt 0.12.3)** - JSON Web Token implementation for webhook authentication
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

**Database (Docker):**
- `MYSQL_ROOT_PASSWORD` - MySQL root password
- `MYSQL_DATABASE` - Database name (default: idverse)
- `MYSQL_USER` - Application database user
- `MYSQL_PASSWORD` - Application database password

**Application Settings:**
- `VERBOSE` - Logging level (DEBUG, INFO, SECRET)
- `TRANSACTION` - Default transaction ID
- `NAME` - Default name for forms

## Project Structure

Standard Maven layout with Spring Boot organization:

```
idverse/
├── src/main/java/org/itnaf/idverse/
│   ├── IdverseApplication.java          # Main Spring Boot application
│   ├── config/
│   │   └── ApiConfig.java                # Configuration (loads .env)
│   ├── controller/
│   │   ├── ApiController.java            # REST API endpoints
│   │   ├── WebController.java            # Web UI controller
│   │   ├── OAuthTestController.java      # OAuth testing endpoints
│   │   └── WebhookController.java        # Webhook receiver
│   ├── service/
│   │   ├── IdVerificationService.java    # Verification logic
│   │   ├── OAuthTokenService.java        # OAuth token management (caching)
│   │   └── SessionService.java           # Session management
│   ├── model/
│   │   ├── VerificationRequest.java      # Request DTO
│   │   ├── VerificationResponse.java     # Response DTO
│   │   ├── VerificationRecord.java       # JPA Entity
│   │   └── OAuthTokenResponse.java       # OAuth response DTO
│   └── repository/
│       └── VerificationRepository.java   # Spring Data JPA repository
├── src/main/resources/
│   ├── application.yml                   # Default configuration (H2)
│   ├── application-docker.yml            # Docker profile (MySQL)
│   ├── templates/                        # Thymeleaf templates
│   └── static/                           # Static assets
├── src/test/java/                        # Test sources
├── docker/                               # Docker-related files
│   └── mysql/
│       ├── conf.d/                       # Custom MySQL config
│       └── init/                         # DB initialization scripts
├── pom.xml                               # Maven configuration
├── Dockerfile                            # Application container image
├── docker-compose.yml                    # Service orchestration
├── .env                                  # Environment variables (gitignored)
├── .env.example                          # Environment template
├── CLAUDE.md                             # This file
├── DOCKER.md                             # Docker deployment guide
└── README.md                             # Project documentation
```

## Development Environment

- **Java Version:** 21
- **Build Tool:** Maven 3.6+
- **IDE:** IntelliJ IDEA (configuration files present in `.idea/`)
- **Local Database:** H2 in-memory (default)
- **Production Database:** MySQL 8.0 (Docker)

## Database Profiles

### Default Profile (Development)
- **Driver:** H2 Database
- **URL:** `jdbc:h2:mem:testdb`
- **Console:** http://localhost:19746/h2-console
- **Auto-schema:** `update`
- **Persistence:** In-memory (data lost on restart)

### Docker Profile (Production)
- **Driver:** MySQL 8.0
- **URL:** `jdbc:mysql://mysql:3306/idverse`
- **Auto-schema:** `update` (configurable via DDL_AUTO env var)
- **Persistence:** Permanent via Docker volume
- **Connection Pool:** HikariCP (max 10 connections)

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
