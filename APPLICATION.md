# Spring Valkey POC

## Overview
A Spring Boot 4.x proof-of-concept demonstrating **Valkey / Redis** integration for caching and distributed locking alongside a MySQL-backed User CRUD API.

## Tech Stack
| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 4.0.4 (Java 21) |
| Web | Spring WebMVC |
| Persistence | Spring Data JPA + MySQL |
| Cache / Lock | Valkey (Redis-compatible) via Spring Data Redis + Lettuce |
| Serialization | Jackson with polymorphic default typing |
| Build | Maven |

## Project Structure
```
com.example.spring_valkey_poc
├── config/               # Valkey / Redis configuration beans
├── controller/           # REST controllers
├── entity/               # JPA entities
├── enums/                # Error code enums
├── exception/            # Custom runtime exceptions
├── handler/              # Global exception handler (@RestControllerAdvice)
├── nonentity/            # Request / Response DTOs
├── records/              # Java records (projections)
├── repository/           # Spring Data JPA repositories
├── service/              # Business service interfaces
│   └── impl/             # Business service implementations
├── cache/
│   ├── repository/       # Generic cache repository abstraction
│   │   └── impl/         # Concrete cache repository implementations
│   └── service/          # Cache service layer
└── lock/                 # Distributed lock abstraction
    └── impl/             # Valkey-based distributed lock implementation
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/v1/users` | Create a new user |
| `GET`  | `/api/v1/users/{id}` | Fetch user by ID (cache-aside) |
| `PUT`  | `/api/v1/users/{id}` | Update user (distributed lock) |

## Caching Strategy
- **Cache-aside (lazy-loading):** On `GET /users/{id}`, the service checks Valkey cache first. On a miss it reads from MySQL, stores the result in cache (TTL 5 min), and returns it.
- **Abstraction:** A generic `CacheRepository<T, ID>` interface and `AbstractCacheRepository` base class encapsulate all Redis operations, letting concrete repos focus on domain logic.

## Distributed Locking

### Implementation
`ValkeyDistributedLockServiceImpl` uses the standard Redis distributed lock pattern:
- **Acquire:** `SET key uuid NX PX leaseTimeMs` — atomic set-if-not-exists with auto-expiry. Returns the UUID to the caller.
- **Release:** Lua script that deletes the key only when its value matches the holder's UUID — prevents one thread from accidentally releasing another's lock.
- **Exception safe:** Both acquire and release catch Redis connection failures gracefully — the lock service never crashes the main flow.

### Where It's Used — User Update (`PUT /api/v1/users/{id}`)
A classic **read → modify → write** problem. Without a lock, concurrent updates to the same user cause **lost writes**:
```
Thread-A reads user (age=25)        Thread-B reads user (age=25)
Thread-A sets age=30, saves          Thread-B sets city=NYC, saves
→ Thread-B's write overwrites Thread-A's age change
```
A per-user lock (`lock:user:update:{id}`) serializes these updates so every change is preserved.

## Configuration (`application.yml`)
```yaml
server:
  port: 10842
  servlet:
    context-path: /spring-valkey-poc

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/users_schema
    username: root
    password:

valkey:
  host: localhost
  port: 6379
  lock:
    lease-time-ms: 5000  # auto-release after this duration
```

## Running & Testing
See [TESTING.md](TESTING.md) for step-by-step setup, all curl commands, and expected responses.


