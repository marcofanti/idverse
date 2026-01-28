# IDVerse Verification Service

A Spring Boot web application that integrates with the IDVerse API to perform phone number verification. The application provides both a REST API and a web interface for submitting verification requests.

## Features

- OAuth 2.0 client credentials authentication with automatic token caching (800 seconds)
- REST API endpoints for programmatic access
- Web UI for manual verification requests
- H2 in-memory database for storing verification history
- Mock OAuth endpoint for testing without external API
- SECRET verbose mode for debugging with full request details
- HTML response detection to prevent error pages from being treated as success

## Prerequisites

- Java 21
- Maven 3.6+
- IDVerse API credentials (client_id and client_secret)

## Configuration

### 1. Set up environment variables

Create a `.env` file in the project root directory with your IDVerse API credentials:

```bash
# Required: IDVerse API Credentials
IDVERSE_CLIENT_ID=your_actual_client_id
IDVERSE_CLIENT_SECRET=your_actual_client_secret
IDVERSE_OAUTH_URL=https://usdemo.idkit.co/api/3.5/oauthToken
IDVERSE_API_URL=https://usdemo.idkit.co/api/3.5/sendSms

# Optional: Logging Level (DEBUG, INFO, WARN, ERROR, or SECRET)
VERBOSE=DEBUG

# Optional: Mock OAuth Token (for testing with mock endpoint)
OAUTHTOKEN=your_test_token_here
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

## Database Console

Access the H2 database console to view stored verification records:

```
http://localhost:19746/h2-console
```

**Connection details:**
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **Username:** `sa`
- **Password:** (leave empty)

## API Endpoints

### Verification API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/verify` | Submit a verification request |
| GET | `/api/verifications` | Get all verification records |
| GET | `/api/verifications/{id}` | Get specific verification by ID |

### Test API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/test/oauth` | Test OAuth token retrieval |
| GET | `/test/config` | Test configuration and get token |
| POST | `/test/oauth/clear` | Clear cached OAuth token |

### Web UI

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Home page with verification form |
| POST | `/verify` | Submit verification via web form |
| GET | `/results` | View verification results |

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

## Development

### Project Structure

```
src/main/java/org/itnaf/idverse/
├── IdverseApplication.java          # Main Spring Boot application
├── config/
│   └── ApiConfig.java                # Configuration (loads .env file)
├── controller/
│   ├── ApiController.java            # REST API endpoints
│   ├── WebController.java            # Web UI controller
│   └── OAuthTestController.java      # OAuth testing endpoints
├── service/
│   ├── IdVerificationService.java    # Verification business logic
│   └── OAuthTokenService.java        # OAuth token management
├── model/
│   ├── VerificationRequest.java      # Request DTO
│   ├── VerificationResponse.java     # Response DTO
│   ├── VerificationRecord.java       # JPA Entity
│   └── OAuthTokenResponse.java       # OAuth response DTO
└── repository/
    └── VerificationRepository.java   # Spring Data JPA repository
```

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

## License

This project is for internal use with the IDVerse API.
