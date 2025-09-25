# Quiz Spring Boot Backend (Full)

This project mirrors the Node.js backend in Java Spring Boot with:
- MongoDB (Spring Data)
- JWT auth
- Email sending (JavaMail)
- File upload (PDF stored as byte[] in user.scores)
- All routes matching original Node API structure

## Run

1. Ensure Java 17 and Maven installed.
2. Set environment variables (recommended):
   - MONGO_URI (e.g. mongodb://localhost:27017/quizdb)
   - JWT_SECRET
   - EMAIL (SMTP username)
   - EMAIL_PASSWORD (SMTP password / app password)
3. Build & run:
   ```bash
   mvn clean package
   mvn spring-boot:run
   ```

Default server: http://localhost:8080

## Routes (same as Node)
See earlier canvas or README for full list.
