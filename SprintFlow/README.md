# SprintFlow CRUD-Only Project

This is the deliberately reduced version of SprintFlow requested for the current project stage.

Included:
- All 7 ER-diagram entities: `User`, `Profile`, `Project`, `Task`, `Bug`, `Comment`, `Notification`
- JPA relationships matching the supplied ER diagram
- MySQL database/table creation through Hibernate
- GET, GET-by-ID, POST, PUT and DELETE APIs for every entity
- Basic React CRUD UI with linked-record dropdowns

Not included:
- Spring Security
- JWT
- OTP verification/login flow
- Email service or notification microservice
- Authentication/authorization

## 1. Required software

- MySQL Server running on port `3306`
- Java 17 or later
- Maven, or Spring Tool Suite with Maven support
- Node.js and npm

The backend is already configured with:

```properties
spring.datasource.username=root
spring.datasource.password=Hello@123
```

The database name is `sprintflow`.

## 2. Database creation

You do not need to manually create the database or tables.

The JDBC URL contains:

```text
createDatabaseIfNotExist=true
```

and Hibernate uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

Therefore, when the backend starts successfully, MySQL creates the `sprintflow` database and Hibernate creates/updates all entity tables.

Important: the MySQL service must be running, and the MySQL `root` password must be `Hello@123`.

## 3. Run backend in Spring Tool Suite

1. Open Spring Tool Suite.
2. Select **File > Import**.
3. Select **Maven > Existing Maven Projects**.
4. Choose the `backend` folder.
5. Finish the import and wait for Maven dependencies to download.
6. Open:
   `src/main/java/com/sprintflow/SprintFlowApplication.java`
7. Right-click it and select **Run As > Spring Boot App**.
8. Wait for the message showing that the application started on port `8080`.

Backend base URL:

```text
http://localhost:8080/api
```

Command-line alternative from the `backend` folder:

```bat
mvn spring-boot:run
```

## 4. Run React UI

Open a second Command Prompt inside the `frontend` folder:

```bat
npm install
npm run dev
```

Then open:

```text
http://localhost:5173
```

Keep both the Spring Boot backend and React frontend running.

## 5. Recommended data creation order

Because the tables use foreign keys, create records in this order:

1. Users
2. Profiles and Projects
3. Tasks and Bugs
4. Comments and Notifications

For deletion, delete dependent records in reverse order. For example, delete a task's comments before deleting the task.

## 6. CRUD endpoints

Each resource supports the same pattern:

| Operation | Method | Example |
|---|---|---|
| Get all | GET | `/api/users` |
| Get one | GET | `/api/users/1` |
| Create | POST | `/api/users` |
| Update | PUT | `/api/users/1` |
| Delete | DELETE | `/api/users/1` |

Resources:

```text
/api/users
/api/profiles
/api/projects
/api/tasks
/api/bugs
/api/comments
/api/notifications
```

## 7. Verify the database in MySQL Workbench

After starting the backend, refresh the Schemas panel and run:

```sql
USE sprintflow;
SHOW TABLES;
```

Expected tables:

```text
users
profiles
projects
tasks
bugs
comments
notifications
```

## 8. Notes

- Password values are plain text only because security was explicitly excluded from this reduced CRUD stage.
- `otpCode`, `otpExpiry` and `otpVerified` remain in the `User` entity because they are part of the supplied ER diagram, but no OTP logic is implemented.
- `Notification` is a normal database entity in this build. It does not send emails.
- A comment must belong to exactly one task or one bug.
