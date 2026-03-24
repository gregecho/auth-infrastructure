# Auth Infrastructure

OAuth 2.1 Authorization Server built with Spring Authorization Server 1.x, Spring Boot 3.3, JDK 21, Gradle, and MongoDB.

## Package Structure

```
com.example.authserver
├── config/       # Spring Security config, DataInitializer, RotatableJwkSource
├── controller/   # MagicCodeController, KeyRotationController
├── grant/        # Custom grant type converters and providers
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
| Issue magic code   | POST   | `/magic-code/issue?username=`         |
| Rotate RSA key     | POST   | `/keys/rotate` (requires ADMIN role)  |

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
- **Client**: `test-client` / `test-secret` — supports `authorization_code`, `refresh_token`, `client_credentials`, `password`, `magic_code`

RSA keys are persisted in MongoDB (`rsa_keys` collection) and survive restarts.

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

### 3. Password

Direct username/password exchange — use only in trusted first-party clients.

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -u "test-client:test-secret" \
  -d "grant_type=password" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "scope=read" | jq
```

### 4. Magic Code

A one-time code flow (passwordless). In production the code would be delivered out-of-band (email/SMS); the issue endpoint returns it directly for development.

**Step 1 — Request a code**

```bash
CODE=$(curl -s -X POST "http://localhost:9000/magic-code/issue?username=admin" | jq -r '.code')
```

**Step 2 — Exchange for a token**

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -u "test-client:test-secret" \
  -d "grant_type=magic_code" \
  -d "code=$CODE" \
  -d "scope=read" | jq
```

Codes expire after 15 minutes and are single-use.

### 5. Refresh Token

```bash
curl -s -X POST http://localhost:9000/oauth2/token \
  -u "test-client:test-secret" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=REFRESH_TOKEN" | jq
```

### 6. Token Introspection

```bash
curl -s -X POST http://localhost:9000/oauth2/introspect \
  -u "test-client:test-secret" \
  -d "token=ACCESS_TOKEN" | jq
```

### 7. Token Revocation

```bash
curl -s -X POST http://localhost:9000/oauth2/revoke \
  -u "test-client:test-secret" \
  -d "token=ACCESS_TOKEN"
```

### 8. OIDC UserInfo

```bash
curl -s http://localhost:9000/userinfo \
  -H "Authorization: Bearer ACCESS_TOKEN" | jq
```

### 9. OIDC Discovery

```bash
curl -s http://localhost:9000/.well-known/openid-configuration | jq
```

### 10. JWK Set

```bash
curl -s http://localhost:9000/oauth2/jwks | jq
```

---

## RSA Key Rotation

Keys are stored in the `rsa_keys` MongoDB collection and survive restarts. On rotation, the new key becomes active for signing; all previous keys are retained so existing tokens remain valid until they expire. The JWKS endpoint (`/oauth2/jwks`) always serves all keys, so resource servers pick up the new signing key automatically.

### How rotation works

1. A new 2048-bit RSA key pair is generated and saved to MongoDB as `active: true`
2. All previous keys are marked `active: false` but **kept** in the collection
3. `RotatableJwkSource` loads all keys ordered newest-first — the active key signs new tokens, older keys validate existing ones
4. Once all tokens signed by an old key have expired, you can safely delete old key documents from the `rsa_keys` collection

### Rotate via curl

Requires HTTP Basic auth with an ADMIN-role user:

```bash
curl -s -X POST http://localhost:9000/keys/rotate \
  -u "admin:admin123" | jq
```

Response:

```json
{ "newKeyId": "3f2a1b..." }
```

### Verify the new key appears in JWKS

```bash
curl -s http://localhost:9000/oauth2/jwks | jq '.keys | length'
```

The count increments by 1 after each rotation.

---

## Running Tests

```bash
./gradlew test
```

## Configuration

| Property | Default |
|---|---|
| `server.port` | `9000` |
| `spring.data.mongodb.uri` | `mongodb://localhost:27017/auth-server` |

---

## Production / Enterprise TODO

### Security (Critical)
- [ ] Enforce HTTPS / TLS — server runs plain HTTP
- [ ] Protect `/magic-code/issue` — add rate limiting and caller verification (API key or internal-network-only)
- [ ] Encrypt RSA private keys at rest — wrap with AES-GCM or delegate to KMS (AWS KMS, HashiCorp Vault)
- [ ] Remove hardcoded `admin123` seed — read from env/secrets manager; skip seeding outside dev profile
- [ ] Prune old RSA keys — add TTL or explicit cleanup after token expiry window to prevent unbounded growth
- [ ] Enforce refresh token rotation on registered clients (`TokenSettings`)
- [ ] Add brute-force protection on `/oauth2/token`

### Security (Important)
- [ ] Add `aud` claim to JWTs so resource servers can verify token audience
- [ ] Include roles/permissions as custom JWT claims (avoid per-request `/userinfo` calls)
- [ ] Configure explicit token TTLs via `OAuth2TokenSettings` (don't rely on defaults)
- [ ] Restrict `test-client` seed to dev profile only

### Reliability & Ops
- [ ] Add `spring-boot-starter-actuator` with `/actuator/health` liveness + readiness probes
- [ ] Add Micrometer + Prometheus metrics (token issuance rates, grant type breakdown, key age)
- [ ] Add distributed tracing (Micrometer Tracing / OpenTelemetry)
- [ ] Configure MongoDB connection pool size and timeouts in `application.yml`
- [ ] Configure graceful shutdown (`server.shutdown=graceful`)

### Multi-instance / HA
- [ ] Fix RSA key init race condition — two instances starting simultaneously can both see `count() == 0`; use a distributed lock (Mongo `findAndModify` upsert or Redisson)
- [ ] Cache `RotatableJwkSource` — every token signing hits MongoDB; add short-lived in-memory cache with invalidation on rotation
- [ ] Replace in-memory form-login sessions with Spring Session + Redis or MongoDB for multi-pod deployments

### Observability & Auditability
- [ ] Structured JSON logging (Logstash encoder) for log aggregation (ELK, Datadog, etc.)
- [ ] Audit log — record token issuance, rotation events, and failed auth attempts to a structured store
- [ ] Emit application events on rotation and key expiry for alerting (webhook, SNS, etc.)

### Configuration & Deployment
- [ ] Separate `application-prod.yml` with secrets externalized (Spring Cloud Config, AWS Parameter Store, k8s Secrets)
- [ ] Dockerfile + `docker-compose.yml` for local dev
- [ ] Helm chart or k8s manifests for production
- [ ] CI/CD pipeline (GitHub Actions) — build, test, image push, deploy
- [ ] Dependency vulnerability scanning (OWASP dependency-check or Dependabot)

### Client Management
- [ ] Enable dynamic client registration (`/connect/register`) or build an admin API
- [ ] Add client secret rotation with expiry
- [ ] Add client-level token TTL and scope configuration via admin API

### Testing
- [ ] Integration tests for `password` and `magic_code` grant types against the token endpoint
- [ ] Unit tests for `RsaKeyServiceImpl` including rotation behavior
- [ ] Tests for `MagicCodeServiceImpl` (expiry, single-use enforcement)
- [ ] Load/soak tests for the token endpoint
