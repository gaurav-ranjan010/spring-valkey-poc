# Spring Valkey POC 🚀

A **Proof of Concept** demonstrating integration of **Valkey** (Redis-compatible in-memory store) with a **Spring Boot 4** application using **Lettuce Connection Factory**. The project implements a JPA-style generic cache repository pattern layered on top of `RedisTemplate`, backed by a **MySQL** database.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Cache Repository Pattern](#cache-repository-pattern)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Reference](#api-reference)
- [Caching Flow](#caching-flow)
- [Key Design Decisions](#key-design-decisions)

---

## Overview

This POC showcases:

- Integrating **Valkey** (Redis-compatible) as a caching layer with **Lettuce** as the connection factory
- A **generic JPA-style cache repository** (`AbstractCacheRepository`) that mirrors `CrudRepository` methods
- A **Cache-Aside pattern** — data is served from Valkey cache on hit; on miss, it falls back to MySQL and populates the cache
- **Self-healing cache** — stale or corrupt cache entries are automatically evicted and the request falls back to the DB transparently
- Proper **Jackson 3.x type metadata** embedded in serialized JSON so objects deserialize back to their correct types (not `LinkedHashMap`)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Cache Store | Valkey / Redis (via `valkey-java 5.4.0`) |
| Cache Connection | Lettuce (non-blocking, thread-safe) |
| Serialization | Jackson 3.x (`tools.jackson`) with `GenericJacksonJsonRedisSerializer` |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL 8 |
| Build Tool | Maven |
| Utilities | Lombok, SLF4J |

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      HTTP Request                       │
└───────────────────────────┬─────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │ UserController │
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │ UserServiceImpl│
                    └───┬───────┬────┘
                        │       │
           ┌────────────▼──┐ ┌──▼──────────────────┐
           │UserDetails    │ │  UserRepository      │
           │CacheService   │ │  (Spring Data JPA)   │
           └────────────┬──┘ └──────────────────────┘
                        │               │
           ┌────────────▼──┐       ┌────▼────┐
           │UserDetails    │       │  MySQL  │
           │CacheRepository│       └─────────┘
           │    Impl       │
           └────────────┬──┘
                        │
           ┌────────────▼──┐
           │AbstractCache  │
           │  Repository   │
           └────────────┬──┘
                        │
                ┌───────▼────────┐
                │  RedisTemplate │ (Lettuce)
                └───────┬────────┘
                        │
                ┌───────▼────────┐
                │    Valkey      │
                │  (port 6379)   │
                └────────────────┘
```

---

## Project Structure

```
src/main/java/com/example/spring_valkey_poc/
│
├── SpringValkeyPocApplication.java          # Entry point
│
├── cache/
│   ├── repository/
│   │   ├── CacheRepository.java             # Generic JPA-style cache interface
│   │   ├── AbstractCacheRepository.java     # Generic implementation over RedisTemplate
│   │   ├── UserDetailsCacheRepository.java  # UserEntity-specific cache interface
│   │   └── impl/
│   │       └── UserDetailsCacheRepositoryImpl.java  # Concrete implementation
│   └── service/
│       └── UserDetailsCacheService.java     # Service facade over cache repository
│
├── config/
│   └── ValkeyConfig.java                    # Lettuce connection factory + RedisTemplate bean
│
├── controller/
│   └── UserController.java                  # REST endpoints
│
├── entity/
│   └── UserEntity.java                      # JPA entity (maps to `user` table)
│
├── enums/
│   └── ErrorCodes.java                      # Application error codes with HTTP status
│
├── exception/
│   └── GlobalException.java                 # Custom runtime exception
│
├── handler/
│   └── GlobalExceptionHandler.java          # @ControllerAdvice exception handler
│
├── nonentity/
│   ├── BaseResponse.java                    # Generic API wrapper
│   ├── ErrorDetails.java                    # Error payload DTO
│   ├── UserDetailsRequest.java              # Inbound request DTO
│   └── UserDetailsResponse.java             # Outbound response DTO
│
├── records/
│   └── UserRecord.java                      # Java record for lightweight projection
│
├── repository/
│   └── UserRepository.java                  # Spring Data JPA repository
│
└── service/
    ├── UserService.java                     # Service interface
    └── impl/
        └── UserServiceImpl.java             # Service implementation (cache-aside logic)
```

---

## Cache Repository Pattern

The cache layer mirrors the JPA `CrudRepository` API so it feels familiar:

| JPA (`CrudRepository`)         | Cache (`CacheRepository`)         |
|-------------------------------|----------------------------------|
| `save(entity)`                | `save(id, entity)`               |
| `findById(id)`                | `findById(id)`                   |
| `findAll()`                   | `findAll()`                      |
| `existsById(id)`              | `existsById(id)`                 |
| `deleteById(id)`              | `deleteById(id)`                 |
| `deleteAll()`                 | `deleteAll()`                    |
| `count()`                     | `count()`                        |
| Custom `findByCity(...)`      | `findAllByCity(city)`            |

### Generic Interface

```java
public interface CacheRepository<T, ID> {
    T save(ID id, T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean existsById(ID id);
    void deleteById(ID id);
    void deleteAll();
    long count();
}
```

### Self-Healing Cache

If a cache entry is stale or corrupt (e.g., format mismatch after a serializer change):
- The bad entry is **automatically evicted** from Valkey
- The request **falls back to MySQL** transparently
- A **WARN log** is emitted for observability

```
WARN - Stale or corrupt cache entry for key 'userDetails::3'.
       Evicting and falling back to DB. Reason: <exception message>
```

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Valkey / Redis | 7.0+ |

> **Note:** Valkey is a Redis-compatible fork. Any Redis 7+ instance works in its place.

### Start Valkey / Redis locally

```bash
# Using Docker
docker run -d --name valkey -p 6379:6379 valkey/valkey:latest

# Or using Homebrew (Redis)
brew install redis && brew services start redis
```

---

## Database Setup

Run the following SQL scripts in order:

```sql
-- 1. Create schema
CREATE SCHEMA IF NOT EXISTS `users_schema`;

-- 2. Create table
CREATE TABLE IF NOT EXISTS `user` (
    `id`         INT          NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(45)  NOT NULL,
    `age`        INT          NOT NULL,
    `city`       VARCHAR(45)  DEFAULT NULL,
    `created_at` TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);
```

Scripts are also available under `src/main/resources/changelog/`.

---

## Configuration

Update `src/main/resources/application.yml` with your local settings:

```yaml
server:
  port: 10842
  servlet:
    context-path: /spring-valkey-poc

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/users_schema?useSSL=false&serverTimezone=UTC
    username: root
    password: <your_mysql_password>
    driver-class-name: com.mysql.cj.jdbc.Driver

valkey:
  host: localhost
  port: 6379
```

### Valkey / Redis Config (`ValkeyConfig.java`)

| Setting | Value | Notes |
|---|---|---|
| Connection | `LettuceConnectionFactory` | Non-blocking, thread-safe |
| Key Serializer | `StringRedisSerializer` | Human-readable keys |
| Value Serializer | `GenericJacksonJsonRedisSerializer` | JSON with embedded type info |
| TTL | 5 minutes (per cache prefix) | Configurable per repository |
| Type Safety | `activateDefaultTyping` enabled | Prevents `LinkedHashMap` deserialization issue |

---

## Running the Application

```bash
# Clone the repository
git clone <repo-url>
cd spring-valkey-poc

# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
```

Application starts at: `http://localhost:10842/spring-valkey-poc`

---

## API Reference

### Base URL

```
http://localhost:10842/spring-valkey-poc/api/v1/users
```

---

### 1. Add User

**`POST /api/v1/users`**

Saves a new user to MySQL.

**Request Body**
```json
{
  "name": "John Doe",
  "age": 28,
  "city": "Mumbai"
}
```

**Response `200 OK`**
```json
{
  "data": {
    "id": 1,
    "name": "John Doe",
    "age": 28,
    "city": "Mumbai"
  },
  "error": null,
  "success": true
}
```

---

### 2. Get User by ID

**`GET /api/v1/users/{id}`**

Fetches a user by ID using **Cache-Aside** strategy:
- ✅ **Cache HIT** → returns from Valkey directly
- ❌ **Cache MISS** → fetches from MySQL, saves to Valkey, returns result

**Response `200 OK`**
```json
{
  "data": {
    "id": 1,
    "name": "John Doe",
    "age": 28,
    "city": "Mumbai"
  },
  "error": null,
  "success": true
}
```

**Response `404 Not Found`**
```json
{
  "data": null,
  "error": {
    "code": 404,
    "message": "User not found"
  },
  "success": false
}
```

---

### Error Codes

| Code | HTTP Status | Message |
|---|---|---|
| `USER_NOT_FOUND` | 404 | User not found |
| `UNABLE_TO_ADD_USER` | 500 | Unable to add user |

---

## Caching Flow

```
GET /api/v1/users/1
        │
        ▼
 Check Valkey cache (key: "userDetails::1")
        │
   ┌────┴─────┐
   │          │
  HIT        MISS
   │          │
   │          ▼
   │    Query MySQL (UserRepository.findById)
   │          │
   │          ▼
   │    Save to Valkey (TTL: 5 min)
   │          │
   └────┬─────┘
        │
        ▼
  Return UserDetailsResponse
```

**Cache key format:** `<cachePrefix>::<id>` → e.g., `userDetails::1`

**Stored value format (with type metadata):**
```json
["com.example.spring_valkey_poc.entity.UserEntity", {"id":1,"name":"John Doe","age":28,"city":"Mumbai",...}]
```

---

## Key Design Decisions

### Why Lettuce over Jedis?

| | Lettuce | Jedis |
|---|---|---|
| Threading | Single shared connection (thread-safe) | Connection-per-thread (pooled) |
| I/O Model | Non-blocking (Netty) | Blocking |
| Async/Reactive | ✅ Supported | ❌ Not supported |
| Spring Boot Default | ✅ Yes | ❌ No |

### Why `activateDefaultTyping`?

Without it, Jackson serializes `UserEntity` as a plain JSON object `{...}`. On deserialization, since `RedisTemplate<String, Object>` has `Object` as the value type, Jackson has no type hint and falls back to `LinkedHashMap`. Enabling `activateDefaultTyping` embeds the class name in the JSON array wrapper so the correct type is always restored.

### Why `NON_FINAL` typing?

`DefaultTyping.NON_FINAL` adds type info to all non-final types (most POJOs, collections), which is the right scope for a general-purpose cache without being overly broad.

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `LinkedHashMap cannot be cast to UserEntity` | `ObjectMapper` missing `activateDefaultTyping` | Already fixed in `ValkeyConfig` |
| `START_OBJECT expected START_ARRAY` | Stale keys from old serializer format in Valkey | Flush keys: `redis-cli --scan --pattern "userDetails::*" \| xargs redis-cli DEL` |
| `Connection refused :6379` | Valkey/Redis not running | Start with Docker or Homebrew |
| `Access denied for user 'root'@'localhost'` | Wrong MySQL credentials | Update `application.yml` password |

