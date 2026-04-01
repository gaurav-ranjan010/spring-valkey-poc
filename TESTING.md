# How to Run & Test

## Prerequisites

| Dependency | Version | Port |
|-----------|---------|------|
| Docker | any | — |
| Java | 21+ | — |
| Maven | 3.9+ | — |
| MySQL | 8.x (Docker) | 3306 |
| Valkey | 8.x (Docker) | 6379 |

---

## 1. Start Infrastructure

### Start Valkey
```bash
docker run -d --name valkey-poc -p 6379:6379 valkey/valkey:8
```

### Start MySQL
```bash
docker run -d --name mysql-poc \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -p 3306:3306 mysql:8
```

### Wait for MySQL to be ready
```bash
sleep 15
docker exec mysql-poc mysqladmin ping -h localhost
# Expected: mysqld is alive
```

### Create schema and table
```bash
docker exec mysql-poc mysql -u root -e "CREATE SCHEMA IF NOT EXISTS users_schema;"

docker exec mysql-poc mysql -u root users_schema -e \
  "CREATE TABLE IF NOT EXISTS user (
    id int NOT NULL AUTO_INCREMENT,
    name varchar(45) NOT NULL,
    age int NOT NULL,
    city varchar(45) DEFAULT NULL,
    created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
  );"
```

---

## 2. Build & Run the Application

```bash
cd spring-valkey-poc
mvn compile
mvn spring-boot:run
```

App starts at: `http://localhost:10842/spring-valkey-poc`

---

## 3. Test API Endpoints

### Test 1 — Create User
```bash
curl -s -X POST http://localhost:10842/spring-valkey-poc/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Atul","age":28,"city":"Delhi"}' | python3 -m json.tool
```
**Expected:** `200` — user created with auto-generated `id`

### Test 2 — Create Another User
```bash
curl -s -X POST http://localhost:10842/spring-valkey-poc/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Raj","age":32,"city":"Mumbai"}' | python3 -m json.tool
```
**Expected:** `200` — second user created

### Test 3 — Fetch User (Cache Miss → DB → Cache)
```bash
curl -s http://localhost:10842/spring-valkey-poc/api/v1/users/1 | python3 -m json.tool
```
**Expected:** `200` — fetched from DB, stored in cache  
**Verify in logs:** `Cache miss for id : 1. Fetching from DB.`

### Test 4 — Fetch Same User (Cache Hit)
```bash
curl -s http://localhost:10842/spring-valkey-poc/api/v1/users/1 | python3 -m json.tool
```
**Expected:** `200` — same data, served from cache  
**Verify in logs:** `Cache hit for id : 1`

### Test 5 — Fetch Non-existent User
```bash
curl -s http://localhost:10842/spring-valkey-poc/api/v1/users/999 | python3 -m json.tool
```
**Expected:** `404` — `"User not found"`

### Test 6 — Update User (Distributed Lock)
```bash
curl -s -X PUT http://localhost:10842/spring-valkey-poc/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Atul Kumar","age":29,"city":"Bangalore"}' | python3 -m json.tool
```
**Expected:** `200` — user updated, lock acquired and released  
**Verify in logs:** `Lock 'lock:user:update:1' acquired` → `Lock 'lock:user:update:1' released`

### Test 7 — Verify Update Persisted in Cache
```bash
curl -s http://localhost:10842/spring-valkey-poc/api/v1/users/1 | python3 -m json.tool
```
**Expected:** `200` — returns updated data (`Atul Kumar`, `Bangalore`)

### Test 8 — Update Non-existent User
```bash
curl -s -X PUT http://localhost:10842/spring-valkey-poc/api/v1/users/999 \
  -H "Content-Type: application/json" \
  -d '{"name":"Ghost","age":0,"city":"Nowhere"}' | python3 -m json.tool
```
**Expected:** `404` — `"User not found"` (lock acquired, but user doesn't exist in DB)

### Test 9 — Concurrent Updates (Lock Contention)
```bash
curl -s -X PUT http://localhost:10842/spring-valkey-poc/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Thread-A","age":30,"city":"Chennai"}' & \
curl -s -X PUT http://localhost:10842/spring-valkey-poc/api/v1/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Thread-B","age":31,"city":"Pune"}' & \
wait
```
**Expected:**
- One returns `200` (acquired the lock, update succeeded)
- Other returns `409` — `"Could not acquire distributed lock. Please retry."`

---

## 4. Verify Valkey State

### Check cached user entries
```bash
docker exec valkey-poc valkey-cli KEYS "userDetails::*"
```

### Check for stale locks (should be empty after all operations)
```bash
docker exec valkey-poc valkey-cli KEYS "lock:*"
```

### Inspect a cached value
```bash
docker exec valkey-poc valkey-cli GET "userDetails::1"
```

### Flush cache (for re-testing)
```bash
docker exec valkey-poc valkey-cli FLUSHDB
```

---

## 5. Cleanup

```bash
# Stop and remove containers
docker rm -f mysql-poc valkey-poc

# Stop the Spring Boot app
lsof -ti:10842 | xargs kill -9
```

