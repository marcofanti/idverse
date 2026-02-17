# IDVerse Verification Service

A Spring Boot web application that integrates with the IDVerse API to perform identity verification. The application provides both a REST API and a web interface for submitting verification requests.

## Features

- OAuth 2.0 client credentials authentication with automatic token caching (800 seconds)
- REST API endpoints for programmatic access
- Web UI for manual verification requests with session authentication
- JWT-authenticated webhook endpoint for receiving IDVerse callbacks
- Dual database support: H2 (development) and MySQL (production)
- Docker Compose deployment with persistent MySQL storage
- Mock OAuth endpoint for testing without external API
- SECRET verbose mode for debugging with full request details
- HTML response detection to prevent error pages from being treated as success

## Deployment Options

### Option 1: Docker (Recommended for Production)

**Prerequisites:**
- Docker Engine 20.10+
- Docker Compose 2.0+

**Quick Start:**

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Edit .env with your IDVerse credentials
# (Required: IDVERSE_CLIENT_ID, IDVERSE_CLIENT_SECRET, AUTH_KEY, JWT_SECRET_KEY)

# 3. Start services
docker-compose up -d

# 4. View logs
docker-compose logs -f

# 5. Access the application
# http://localhost:19746
```

**Features:**
- ✅ Permanent MySQL database with persistent storage
- ✅ Automatic service orchestration
- ✅ Production-ready configuration
- ✅ Health checks and automatic restarts
- ✅ Data survives container restarts

**See [DOCKER.md](DOCKER.md) for complete Docker deployment guide.**

### Option 2: Local Development

**Prerequisites:**
- Java 21
- Maven 3.6+
- IDVerse API credentials (client_id and client_secret)

## Configuration

### 1. Set up environment variables

Create a `.env` file in the project root directory:

```bash
# Copy the template
cp .env.example .env
```

Edit the `.env` file with your configuration:

```bash
# IDVerse API Credentials (Required)
IDVERSE_CLIENT_ID=your_actual_client_id
IDVERSE_CLIENT_SECRET=your_actual_client_secret
IDVERSE_OAUTH_URL=https://us.demo.idkit.co/api/3.5/oauthToken
IDVERSE_API_URL=https://us.demo.idkit.co/api/3.5/sendSms

# Webhook Configuration
NOTIFY_URL_COMPLETE=http://localhost:19746/api/webhook
NOTIFY_URL_EVENT=http://localhost:19746/api/webhook
JWT_SECRET_KEY=your_jwt_secret_key_here

# Web Interface Authentication (Required)
AUTH_KEY=your_authentication_key_here

# Logging Level (DEBUG, INFO, WARN, ERROR, or SECRET)
VERBOSE=DEBUG

# Mock OAuth Token (for testing with mock endpoint)
OAUTHTOKEN=your_test_token_here

# Default Form Values (Optional)
TRANSACTION=default_transaction_id
NAME=Default Name
SUPPLIED_FIRST_NAME=Default First Name

# MySQL Configuration (Docker only)
MYSQL_ROOT_PASSWORD=rootpassword
MYSQL_DATABASE=idverse
MYSQL_USER=idverse_user
MYSQL_PASSWORD=idverse_pass
MYSQL_PORT=3306

# Application Settings (Docker)
APP_PORT=19746
DDL_AUTO=update
SHOW_SQL=false
```

**Note:** The `.env` file is already in `.gitignore` to prevent accidentally committing credentials.

#### Verbose Modes

- **DEBUG** (default): Detailed logging with sensitive data masked (e.g., `client_secret: abc****xyz`)
- **INFO**: Standard application logging
- **SECRET**: Complete POST requests written to **console only** (System.out) with all keys unmasked
  - Includes full `client_secret`, `access_token`, and all request parameters
  - Use for debugging authentication issues
  - **WARNING**: Sensitive data is NOT masked in this mode

### 2. Build the application

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The application will start on port **19746** (configurable in `application.yml`).

## OAuth Token Caching

The application automatically caches OAuth tokens for **800 seconds** (13.3 minutes) to reduce API calls:

- First request: Token is fetched from the OAuth server
- Subsequent requests: Cached token is reused until expiration
- After 800 seconds: New token is automatically fetched
- Token caching is thread-safe and works across all API requests

**Benefits:**
- Reduces load on OAuth server
- Improves response time for verification requests
- Automatic renewal when token expires

**Manual token management:**
```bash
# Clear cached token (forces refresh on next request)
curl -X POST http://localhost:19746/test/clear-token
```

## Mock OAuth Endpoint

For testing without calling the real OAuth server, use the built-in mock endpoint:

**1. Set mock token in `.env`:**
```bash
OAUTHTOKEN=eyJ0eXAiOiJKV1QiLCJhbGciOi...your_test_token
```

**2. Call the mock endpoint:**
```bash
curl -X POST http://localhost:19746/api/3.5/oauthToken
```

**Response:**
```json
{
  "token_type": "Bearer",
  "expires_in": 900,
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOi...your_test_token"
}
```

This endpoint mimics the real IDVerse OAuth server response format.

## Testing

### Test 1: Verify Configuration and OAuth Token

Before running any verification requests, test that your `.env` configuration is correct and that you can obtain an OAuth token.

**Using curl:**

```bash
curl http://localhost:19746/test/oauth
```

**Expected successful response:**

```json
{
  "status": "SUCCESS",
  "message": "OAuth token obtained successfully",
  "token_type": "Bearer",
  "expires_in": 900,
  "access_token_preview": "eyf...0k..."
}
```

**Expected error response (if credentials are invalid):**

```json
{
  "status": "FAILURE",
  "error": "invalid_client",
  "error_description": "Client authentication failed",
  "hint": "Check your client_id and client_secret",
  "message": "Client authentication failed"
}
```

**Alternative test endpoint:**

```bash
curl http://localhost:19746/test/config
```

This endpoint tests the configuration and attempts to obtain a token in one call.

**Verbose Debug Mode:**

To see detailed request parameters and raw API responses, add `?verbose=debug` to the OAuth test endpoint:

```bash
curl "http://localhost:19746/test/oauth?verbose=debug"
```

**Verbose debug response includes:**

- **Request details**: URL, HTTP method, content type, and all request parameters (with masked client_secret)
- **Raw API response**: The complete JSON response from the IDVerse OAuth endpoint
- **Full access token**: The complete JWT token (not just a preview)
- **Parsed response**: All parsed fields from the OAuth response

**Example verbose response:**

```json
{
  "request": {
    "url": "https://us.demo.idkit.co/api/3.5/oauthToken",
    "method": "POST",
    "content_type": "application/x-www-form-urlencoded",
    "parameters": {
      "grant_type": "client_credentials",
      "client_id": "2766694721",
      "client_secret": "yo3r****Mc8T"
    }
  },
  "raw_response": "{\"token_type\":\"Bearer\",\"expires_in\":900,\"access_token\":\"eyJ0eXAi...\"}",
  "status": "SUCCESS",
  "message": "OAuth token obtained successfully",
  "token_type": "Bearer",
  "expires_in": 900,
  "access_token_preview": "eyJ0eXAiOiJKV1QiLCJh...",
  "access_token_full": "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImp0aSI6..."
}
```

**Server logs in verbose mode:**

When `verbose=debug` is used, the server will also log detailed information:

```
=== OAuth Request Details ===
URL: https://us.demo.idkit.co/api/3.5/oauthToken
Method: POST
Content-Type: application/x-www-form-urlencoded
Parameters:
  grant_type: client_credentials
  client_id: 2766694721
  client_secret: yo3r****Mc8T
=============================

=== OAuth Raw Response ===
Response Body: {"token_type":"Bearer","expires_in":900,"access_token":"..."}
==========================

OAuth token obtained successfully
Token Type: Bearer
Expires In: 900 seconds
Access Token: eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImp0aSI6...
```

### Test 2: Web UI

Open your browser and navigate to:

```
http://localhost:19746
```

1. Enter a phone number in international format (e.g., `+1234567890`)
2. Enter a reference ID (e.g., `REF123`)
3. Click "Verify"
4. View the results page

### Test 3: REST API - Submit Verification

**Request:**

```bash
curl -X POST http://localhost:19746/api/verify \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890",
    "referenceId": "REF123"
  }'
```

**Successful response:**

```json
{
  "id": 1,
  "phoneNumber": "+1234567890",
  "referenceId": "REF123",
  "apiResponse": "{...}",
  "status": "SUCCESS",
  "timestamp": "2026-01-28T12:00:00",
  "errorMessage": null
}
```

**Validation error response:**

```json
{
  "phoneNumber": "Invalid phone number format. Use international format (e.g., +1234567890)",
  "referenceId": "Reference ID is required"
}
```

### Test 4: REST API - Get All Verifications

```bash
curl http://localhost:19746/api/verifications
```

**Response:**

```json
[
  {
    "id": 1,
    "phoneNumber": "+1234567890",
    "referenceId": "REF123",
    "apiResponse": "{...}",
    "status": "SUCCESS",
    "timestamp": "2026-01-28T12:00:00",
    "errorMessage": null
  }
]
```

### Test 5: REST API - Get Specific Verification

```bash
curl http://localhost:19746/api/verifications/1
```

### Test 6: Clear OAuth Token Cache

If you need to force a new token retrieval (e.g., after updating credentials):

```bash
curl -X POST http://localhost:19746/test/oauth/clear
```

## Database Access

### H2 Console (Local Development)

When running locally with the default profile, access the H2 database console:

```
http://localhost:19746/h2-console
```

**Connection details:**
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **Username:** `sa`
- **Password:** `sa`

### MySQL (Docker Deployment)

When running via Docker, connect to MySQL:

```bash
# Via Docker Compose
docker-compose exec mysql mysql -u idverse_user -p
# Password: idverse_pass (or your custom password from .env)

# Via external client (e.g., MySQL Workbench, DBeaver)
# Host: localhost
# Port: 3306
# Database: idverse
# Username: idverse_user
# Password: idverse_pass
```

## API Endpoints

### Verification API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/verify` | Submit a verification request |
| GET | `/api/verifications` | Get all verification records |
| GET | `/api/verifications/{id}` | Get specific verification by ID |

### Webhook API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/webhook` | Receive webhook callbacks from IDVerse (JWT authenticated) |

**Webhook Authentication:**
- Requires `Authorization: Bearer <JWT_TOKEN>` header
- JWT must be signed with `JWT_SECRET_KEY` from `.env`
- Used by IDVerse to send verification status updates

### Test API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/test/oauth` | Test OAuth token retrieval |
| GET | `/test/oauth?verbose=debug` | Verbose OAuth test with full details |
| GET | `/test/config` | Test configuration and get token |
| POST | `/test/oauth/clear` | Clear cached OAuth token |
| POST | `/api/3.5/oauthToken` | Mock OAuth endpoint (returns OAUTHTOKEN from .env) |

### Web UI

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page with verification form (requires AUTH_KEY) |
| POST | `/verify` | Submit verification via web form |
| GET | `/results` | View verification results |
| POST | `/login` | Authenticate with AUTH_KEY |
| POST | `/logout` | Clear session |

## OAuth Token Management

The application automatically manages OAuth tokens:

- Tokens are cached after the first successful retrieval
- Tokens are valid for 15 minutes (900 seconds)
- The application automatically renews tokens when they expire in less than 60 seconds
- You can manually clear the token cache using the `/test/oauth/clear` endpoint

## Troubleshooting

### OAuth Token Errors

**Error:** `IDVERSE_CLIENT_ID not configured - running in DEMO MODE`

**Solution:** Ensure your `.env` file exists and contains valid credentials. Restart the application after updating the file.

**Error:** `Client authentication failed`

**Solution:** Verify that your `IDVERSE_CLIENT_ID` and `IDVERSE_CLIENT_SECRET` are correct.

**Error:** `Connection timeout`

**Solution:** Check that the `IDVERSE_OAUTH_URL` is correct and that you have network access to the IDVerse API.

### Validation Errors

**Error:** `Invalid phone number format`

**Solution:** Use international format with a leading `+` and country code (e.g., `+1234567890`).

**Error:** `Reference ID is required`

**Solution:** Provide a non-empty reference ID.

### Docker Issues

**Error:** `Application can't connect to MySQL`

**Solution:**
1. Check MySQL health: `docker-compose ps`
2. View MySQL logs: `docker-compose logs mysql`
3. Verify MySQL is healthy before app starts (health check should handle this)
4. Check credentials in `.env` match `docker-compose.yml`

**Error:** `Port already in use` (19746 or 3306)

**Solution:** Change ports in `.env`:
```bash
APP_PORT=8080
MYSQL_PORT=3307
```

**Error:** `Database connection refused`

**Solution:** Ensure MySQL container is healthy:
```bash
docker-compose exec mysql mysqladmin ping -h localhost -u root -p
```

### Session/Authentication Issues

**Error:** `Unauthorized access to web interface`

**Solution:** Make sure you're logged in with the correct `AUTH_KEY` from `.env`.

**Error:** `Webhook authentication failed`

**Solution:** Ensure the JWT token in the webhook request is signed with the `JWT_SECRET_KEY` from `.env`.

## Development

### Project Structure

```
idverse/
├── src/main/java/org/itnaf/idverse/
│   ├── IdverseApplication.java          # Main Spring Boot application
│   ├── config/
│   │   └── ApiConfig.java                # Configuration (loads .env file)
│   ├── controller/
│   │   ├── ApiController.java            # REST API endpoints
│   │   ├── WebController.java            # Web UI controller
│   │   ├── WebhookController.java        # Webhook receiver (JWT auth)
│   │   └── OAuthTestController.java      # OAuth testing endpoints
│   ├── service/
│   │   ├── IdVerificationService.java    # Verification business logic
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
│   ├── application.yml                   # Default config (H2 database)
│   ├── application-docker.yml            # Docker profile (MySQL)
│   ├── templates/                        # Thymeleaf templates
│   └── static/                           # Static assets
├── docker/                               # Docker-related files
│   └── mysql/
│       ├── conf.d/                       # Custom MySQL config
│       └── init/                         # DB initialization scripts
├── lib/
│   └── idverse-api-1.0-SNAPSHOT.jar     # Pre-built API client library (committed)
├── pom.xml                               # Maven configuration
├── Dockerfile                            # Application container image
├── docker-compose.yml                    # Service orchestration
├── .env                                  # Environment variables (gitignored)
├── .env.example                          # Environment template
├── .dockerignore                         # Docker build context exclusions
├── CLAUDE.md                             # Development guide for Claude Code
├── DOCKER.md                             # Docker deployment guide
└── README.md                             # This file
```

### Technology Stack

- **Framework:** Spring Boot 3.2.1
- **Java Version:** 21
- **Database:** H2 (dev) / MySQL 8.0 (prod)
- **Template Engine:** Thymeleaf
- **HTTP Client:** WebFlux WebClient (via `idverse-api`)
- **API Client:** `idverse-api` library — bundled as `lib/idverse-api-1.0-SNAPSHOT.jar`
- **Security:** JWT (jjwt 0.12.3), Session-based auth
- **Build Tool:** Maven
- **Container:** Docker + Docker Compose

### Build Commands

```bash
# Build the project
mvn clean install

# Compile only
mvn compile

# Run tests
mvn test

# Package the application
mvn package

# Skip tests during build
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

## Recent Features & Improvements

### Authentication & Security
- **JWT Webhook Authentication** - Secure webhook endpoint with JWT token validation
- **Session Management** - Web interface protected with AUTH_KEY authentication
- **OAuth Token Caching** - 800-second token cache reduces API calls and improves performance

### Database & Deployment
- **MySQL Support** - Production-ready MySQL database integration
- **Docker Deployment** - Complete Docker Compose setup with persistent storage
- **Multi-stage Dockerfile** - Optimized container images for production
- **Database Persistence** - Named volumes ensure data survives container restarts
- **Health Checks** - MySQL health monitoring with automatic app startup sequencing

### Developer Experience
- **Spring Profiles** - Automatic environment switching (H2 for dev, MySQL for Docker)
- **Environment Configuration** - All sensitive values externalized to `.env` file
- **Comprehensive Documentation** - Detailed guides (README.md, DOCKER.md, CLAUDE.md)
- **Mock Endpoints** - Test OAuth flow without calling external APIs

### Webhook Integration
- **Webhook Receiver** - `/api/webhook` endpoint for IDVerse callbacks
- **Event Tracking** - Store and display webhook events in verification history
- **Status Updates** - Real-time verification status updates via webhooks

## Architecture Decisions

### Why Docker?
- **Consistency** - Same environment across dev, staging, and production
- **Isolation** - Services run in isolated containers with dedicated networking
- **Persistence** - Data survives container restarts via named volumes
- **Scalability** - Easy to add more services (Redis, monitoring, etc.)

### Why MySQL for Production?
- **Reliability** - Production-tested, ACID-compliant database
- **Persistence** - Data survives application restarts (unlike H2)
- **Performance** - Connection pooling and optimized queries
- **Familiarity** - Well-known database with extensive tooling support

### Why H2 for Development?
- **Speed** - Instant startup, no external dependencies
- **Convenience** - Built-in web console for quick debugging
- **Simplicity** - No configuration needed for local development
- **Reset** - Easy to reset database (just restart app)

## Production Deployment Checklist

Before deploying to production:

- [ ] Change all default passwords in `.env`
- [ ] Set strong `AUTH_KEY` and `JWT_SECRET_KEY`
- [ ] Use real `IDVERSE_CLIENT_ID` and `IDVERSE_CLIENT_SECRET`
- [ ] Set `DDL_AUTO=validate` to prevent automatic schema changes
- [ ] Enable MySQL SSL connections
- [ ] Set up regular database backups
- [ ] Configure proper logging (not SECRET mode)
- [ ] Set up monitoring and alerting
- [ ] Use Docker secrets or external secrets manager
- [ ] Review and harden network security
- [ ] Set resource limits in `docker-compose.yml`
- [ ] Test webhook authentication with real IDVerse callbacks
- [ ] Set up HTTPS/TLS for production endpoints

## License

This project is for internal use with the IDVerse API.
