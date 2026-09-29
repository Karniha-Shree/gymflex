# GymFlex — Gym Membership and Attendance Tracker (Backend)

Spring Boot REST API for managing gym members, plans, memberships and daily check-ins.

| Item | Value |
|---|---|
| Group / Artifact / Version | `com.gymflex` / `gymflex-backend` / `0.0.1-SNAPSHOT` |
| Java | 21 |
| Spring Boot | 3.5.6 (via `spring-boot-starter-parent`; all other versions are managed by it) |
| Database | MySQL 8.x, database name `gymflex` |
| Build tool | Maven 3.6.3 or newer |

Architecture: `Controller -> Service -> Repository -> MySQL`. All business rules live in the service layer.

---
## 1. Requirements
- JDK 21
- MySQL Server 8.x running on `localhost:3306`
- Spring Tools 4 (Eclipse based) — it includes Maven, so you do not need to install Maven separately
- (Optional) standalone Maven 3.6.3+ for command-line builds

## 2. Java 21 setup
Check with `java -version` (should say 21). In Spring Tools:
`Window -> Preferences -> Java -> Installed JREs` -> add your JDK 21 and tick it.
Then right-click the project -> `Properties -> Java Compiler` -> compliance level **21**.

## 3. MySQL setup and 4. Database creation
```sql
-- optional: the app also creates the database automatically (createDatabaseIfNotExist=true)
CREATE DATABASE IF NOT EXISTS gymflex CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- optional: a dedicated user instead of root
CREATE USER IF NOT EXISTS 'gymflex_user'@'localhost' IDENTIFIED BY 'choose_a_password';
GRANT ALL PRIVILEGES ON gymflex.* TO 'gymflex_user'@'localhost';
FLUSH PRIVILEGES;
```
Tables are created automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

## 5. Configure application.properties
Open `src/main/resources/application.properties` and edit:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD      # <-- type your MySQL password here
```

## 6. Import into Spring Tools
`File -> Import -> Maven -> Existing Maven Projects -> Browse` -> select the unzipped `GymFlex-Backend` folder -> `Finish`.
Wait for the workspace build (bottom-right progress bar). If dependencies do not download,
right-click the project -> `Maven -> Update Project...` -> tick **Force Update of Snapshots/Releases** -> OK.

## 7. Run
- Spring Tools: right-click `GymflexApplication.java` -> `Run As -> Spring Boot App` (or `Java Application`).
- Command line: `mvn spring-boot:run`

The API is at `http://localhost:8080`. Quick test: open `http://localhost:8080/api/dashboard` in the browser.

## 8. Maven commands
```
mvn clean
mvn test
mvn package          # creates target/gymflex-backend-0.0.1-SNAPSHOT.jar
java -jar target/gymflex-backend-0.0.1-SNAPSHOT.jar
```
Tests use an in-memory H2 database, so they do **not** need MySQL.

> Maven Wrapper (`mvnw`) is not included. To add it, run `mvn wrapper:wrapper` once (needs internet).

## 9. API endpoints
| Method | URL | Description |
|---|---|---|
| POST | `/api/members` | Register a member |
| GET | `/api/members?search=` | List members (optional search by name/email/phone) |
| GET | `/api/members/{id}` | Get one member |
| PUT | `/api/members/{id}` | Update member |
| DELETE | `/api/members/{id}` | Delete member (also deletes their memberships and check-ins) |
| POST | `/api/plans` | Create plan |
| GET | `/api/plans` | List plans |
| GET | `/api/plans/{id}` | Get plan |
| PUT | `/api/plans/{id}` | Update plan (extra, used by the frontend) |
| POST | `/api/memberships` | Create membership (`expiryDate` optional; default = start + plan duration) |
| GET | `/api/memberships` | List memberships |
| GET | `/api/memberships/{id}` | Get membership |
| PUT | `/api/memberships/{id}/renew` | Renew (body optional: `{"planId": 2}`) |
| GET | `/api/memberships/expiring-soon` | Active memberships expiring in the next 7 days |
| POST | `/api/checkins` | Check in `{"memberId": 1}` |
| GET | `/api/checkins/member/{memberId}` | Member's check-ins |
| GET | `/api/checkins/member/{memberId}/current-month` | Check-in count for the current month |
| GET | `/api/dashboard` | Summary counts |

### Business rules
1. **Expired = no check-in.** Checked in `CheckInService` before saving. Rejected with HTTP 400 and a clear message.
   Only one check-in per member per day is allowed (HTTP 409 on a second attempt).
2. **Renewal extends from the current expiry date.** Expiry 15 Oct + Monthly -> 15 Nov (not 28 Oct).
   If the membership already expired, the new period starts today. Cancelled memberships cannot be renewed.
3. **Expiring soon** = ACTIVE with expiry between today and today + 7 days (inclusive).
4. **Current-month attendance** = check-ins between the 1st and last day of the current month.

### Example requests
```
POST /api/members
{"name":"Test User","email":"test@example.com","phone":"9876543210","dateOfBirth":"2000-01-31","address":"Salem","emergencyContact":"Parent - 9876500000"}

POST /api/memberships
{"memberId":1,"planId":1,"startDate":"2026-09-28"}

PUT /api/memberships/1/renew

POST /api/checkins
{"memberId":1}
```
Error format:
```json
{"timestamp":"2026-09-28T10:00:00","status":400,"error":"Bad Request","message":"Check-in rejected: membership expired on 2026-09-20. Please renew the membership."}
```
Validation errors also include `fieldErrors` (e.g. `{"email":"Email must be a valid email address"}`).

## 10. Swagger
Swagger/OpenAPI is **not included** (it adds a dependency that must match the Spring Boot version).
Test the API with Postman, the browser (GET requests) or the frontend.

## 11. Connect the frontend
Start this backend, then open the separate `GymFlex-Frontend` project (VS Code + Live Server, port 5500).
Allowed origins are set in `application.properties`:
```properties
gymflex.cors.allowed-origins=http://localhost:5500,http://127.0.0.1:5500
```
To allow another origin add it to the comma separated list (no spaces) and restart the backend.

## Seed data
On first start (when a table is empty) sample plans, 6 members, memberships (active, expiring soon, expired) and check-ins are inserted.
Existing data is never duplicated. To turn it off: `gymflex.seed.enabled=false`.
To start from scratch: `DROP DATABASE gymflex;` and restart the app.

## 12. Common errors and solutions
| Problem | Solution |
|---|---|
| `Access denied for user 'root'@'localhost'` | Wrong password in `application.properties`. |
| `Communications link failure` / `Connection refused` | MySQL is not running or not on port 3306. |
| `Port 8080 was already in use` | Stop the other app or add `server.port=8081` (and update the frontend `API_BASE_URL`). |
| `release version 21 not supported` / wrong Java | Spring Tools is using an older JDK. Set JDK 21 (section 2) and run `Maven -> Update Project`. |
| Red errors / missing dependencies in Spring Tools | Check internet, then `Maven -> Update Project -> Force Update`. Delete a corrupt `~/.m2/repository` folder if needed. |
| Browser shows a CORS error | Open the frontend through Live Server (`http://127.0.0.1:5500`), not by double-clicking the file, or add your origin to `gymflex.cors.allowed-origins`. |
| Check-in returns "membership expired" | That is the business rule working. Renew the membership first. |
