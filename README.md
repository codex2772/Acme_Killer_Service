# JewelERP - Cloud-Native Jewelry Management System

[![Build Status](https://github.com/aurajewels/jewel-erp/workflows/CI/CD/badge.svg)](https://github.com/aurajewels/jewel-erp/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

A comprehensive cloud-native ERP backend for jewelry retailers featuring inventory tracking, GST-compliant billing, and stock management.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- AWS CLI (for deployment)

### Local Development

```bash
# Clone the repository
git clone https://github.com/aurajewels/jewel-erp.git
cd jewel-erp

# Run the application
./mvnw spring-boot:run

# Or build and run
./mvnw clean package -DskipTests
java -jar target/jewel-erp-0.0.1.jar
```

### Using Docker

```bash
# Build and run
docker build -t jewel-erp:latest .
docker run -p 8080:8080 jewel-erp:latest

# Or use docker-compose
docker-compose up --build
```

## 📚 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api` | GET | Welcome message |
| `/api/hello` | GET | Hello World |
| `/actuator/health` | GET | Health status |

## 🏗️ Project Structure

```
src/main/java/com/aurajewels/jewel/
├── controller/          # REST controllers
├── service/             # Business logic (coming soon)
├── repository/          # Data access (coming soon)
└── entity/              # Domain entities (coming soon)
```

## 🔧 Configuration

Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |
| `DATABASE_URL` | PostgreSQL URL | - |
| `DATABASE_USERNAME` | DB username | - |
| `DATABASE_PASSWORD` | DB password | - |

## 🐳 Deployment (AWS ECS)

### Prerequisites
1. AWS CLI configured
2. ECR repository created
3. ECS Cluster (Fargate) set up
4. ALB with target group

### Deploy Steps

```bash
# 1. Login to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin $ECR_REGISTRY

# 2. Build and push image
docker build -t jewel-erp:latest .
docker tag jewel-erp:latest $ECR_REGISTRY/jewel-erp:latest
docker push $ECR_REGISTRY/jewel-erp:latest

# 3. Register task definition
aws ecs register-task-definition \
  --cli-input-json file://deploy/ecs/task-definition.json

# 4. Update service
aws ecs update-service \
  --cluster jewel-erp-cluster \
  --service jewel-erp-service \
  --force-new-deployment
```

See [deploy/ecs/README.md](deploy/ecs/README.md) for detailed instructions.

### Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Format code
./mvnw spotless:apply
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.
