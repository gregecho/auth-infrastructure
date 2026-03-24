# Auth Infrastructure

OAuth 2.1 Authorization Server built with Spring Authorization Server 1.x, Spring Boot 3.3, JDK 21, Gradle, and MongoDB.

## Package Structure

```
com.example.authserver
├── config/       # Spring Security config, DataInitializer
├── service/      # Business logic, implements Spring AS interfaces
├── repository/   # Spring Data Mongo repositories
└── model/        # MongoDB document entities
```

## OAuth 2.1 Endpoints

| Endpoint           | Method | Path                                  |
|--------------------|--------|---------------------------------------|
| Authorization      | GET    | `/oauth2/authorize`                   |
| Token              | POST   | `/oauth2/token`                       |
| Introspection      | POST   | `/oauth2/introspect`                  |
| Revocation         | POST   | `/oauth2/revoke`                      |
| JWK Set            | GET    | `/oauth2/jwks`                        |
| OIDC Discovery     | GET    | `/.well-known/openid-configuration`   |
| UserInfo           | GET    | `/userinfo`                           |

## Prerequisites

- JDK 21
- MongoDB (or Docker)

## Quick Start

```bash
docker run -d -p 27017:27017 mongo
./gradlew bootRun
```

On startup, `DataInitializer` seeds:
- **User**: `admin` / `admin123`
- **Client**: `test-client` / `test-secret`

---

## Grant Type Examples

### 1. Client Credentials

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -u "test-client:test-secret" \
  -d "grant_type=client_credentials&scope=read" | jq
```

### 2. Authorization Code + PKCE

**Step 1 — Generate PKCE values**

```bash
CODE_VERIFIER=$(openssl rand -base64 32 | tr -d '=+/' | cut -c1-43)
CODE_CHALLENGE=$(echo -n "$CODE_VERIFIER" | openssl dgst -sha256 -binary | openssl base64 | tr '+/' '-_' | tr -d '=')
echo "verifier=$CODE_VERIFIER"
echo "challenge=$CODE_CHALLENGE"
```

**Step 2 — Open in browser (user logs in with admin/admin123)**

```
http://localhost:9000/oauth2/authorize?response_type=code&client_id=test-client&redirect_uri=http://localhost:8080/login/oauth2/code/test-client&scope=openid%20profile%20read&code_challenge=CODE_CHALLENGE&code_challenge_method=S256
```

**Step 3 — Exchange code for tokens**

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -u "test-client:test-secret" \
  -d "grant_type=authorization_code" \
  -d "code=AUTH_CODE" \
  -d "redirect_uri=http://localhost:8080/login/oauth2/code/test-client" \
  -d "code_verifier=CODE_VERIFIER" | jq
```

### 3. Refresh Token

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -u "test-client:test-secret" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=REFRESH_TOKEN" | jq
```

### 4. Token Introspection

```bash
curl -s -X POST http://localhost:9000/oauth2/introspect \
  -u "test-client:test-secret" \
  -d "token=ACCESS_TOKEN" | jq
```

### 5. Token Revocation

```bash
curl -s -X POST http://localhost:9000/oauth2/revoke \
  -u "test-client:test-secret" \
  -d "token=ACCESS_TOKEN"
```

### 6. OIDC UserInfo

```bash
curl -s http://localhost:9000/userinfo \
  -H "Authorization: Bearer ACCESS_TOKEN" | jq
```

### 7. OIDC Discovery

```bash
curl -s http://localhost:9000/.well-known/openid-configuration | jq
```

### 8. JWK Set

```bash
curl -s http://localhost:9000/oauth2/jwks | jq
```

---

## Running Tests

```bash
# Unit tests (Spock + Mockito, no DB needed) + integration tests (embedded MongoDB)
./gradlew test
```

## Configuration

| Property | Default |
|---|---|
| `server.port` | `9000` |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/auth-server` |

> **Note**: RSA key pair is generated in-memory on startup. For production, load from a persistent keystore.
