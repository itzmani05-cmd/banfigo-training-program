# Week 1 Backend Assessment

A basic Spring Boot REST API project developed as part of the Week 1 Backend Assessment. This project demonstrates Spring Boot project setup, REST API development, PostgreSQL integration, Git workflow, and API testing using Postman.

---

## 🚀 Technologies Used

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- PostgreSQL
- Maven
- Git & GitHub
- Postman

---

## 📁 Project Structure

```
src
├── main
│   ├── java
│   │   └── com.example.week1_banfigo
│   │       ├── controller
│   │       ├── repository
│   │       ├── entity
│   │       └── Week1BanfigoApplication.java
│   └── resources
│       └── application.properties
└── test
```

---

## ⚙️ Prerequisites

Before running the project, ensure you have installed:

- Java 21
- Maven
- PostgreSQL
- Git
- Postman
- VS Code / IntelliJ IDEA

---

## 🛠️ Database Configuration

Create a PostgreSQL database named:

```
student_db
```

Update `application.properties`:

```properties
spring.application.name=week1-banfigo

server.port=9999

spring.datasource.url=jdbc:postgresql://localhost:5432/student_db
spring.datasource.username=postgres
spring.datasource.password=your_password

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Replace `your_password` with your PostgreSQL password.

---

## ▶️ Running the Application

Clone the repository:

```bash
git clone https://github.com/your-username/week1-banfigo.git
```

Navigate to the project:

```bash
cd week1-banfigo
```

Run the application:

```bash
./mvnw spring-boot:run
```

For Windows:

```bash
mvnw.cmd spring-boot:run
```

The application will start at:

```
http://localhost:9999
```

---

## 📌 API Endpoints

### Health Check

```
GET /health
```

Example:

```
GET http://localhost:9999/health
```

Response

```text
Application is running successfully!
```

---

### Project Information

```
GET /api/info
```

Example

```
GET http://localhost:9999/api/info
```

Response

```json
{
  "projectName": "Week 1 Backend Assessment",
  "studentName": "Your Name",
  "version": "1.0.0"
}
```

---

## 🗄️ Database

The application uses PostgreSQL with Spring Data JPA.

Hibernate automatically creates the required tables using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

---

## 🧪 API Testing

Use Postman to test the APIs.

Available APIs:

- GET `/health`
- GET `/api/info`

---

## 📦 Git Workflow

```bash
git init

git add .

git commit -m "Initial Spring Boot backend setup"

git branch -M main

git remote add origin https://github.com/your-username/week1-banfigo.git

git push -u origin main
```

---

## 👨‍💻 Author

**Manikandan M**

Backend Assessment - Week 1

Government College of Technology, Coimbatore
