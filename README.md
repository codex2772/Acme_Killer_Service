# JewelERP - Cloud-Native Jewelry Management System

[![Build Status](https://github.com/aurajewels/jewel-erp/workflows/CI/CD/badge.svg)](https://github.com/aurajewels/jewel-erp/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen.svg)](https://spring.io/projects/spring-boot)

A comprehensive cloud-native ERP backend for jewelry retailers featuring inventory tracking, GST-compliant billing, and stock management.

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- PostgreSQL 16+ (or use Docker)

### Local Development

```bash
# Clone the repository
git clone https://github.com/aurajewels/jewel-erp.git
cd jewel-erp

# Start dependencies (PostgreSQL)
docker-compose up -d postgres

# Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Or build and run
./mvnw clean package -DskipTests
java -jar target/jewel-erp-0.0.1-SNAPSHOT.jar
```

### Using Docker

```bash
# Build and run everything
docker-compose up --build

# Or just build the image
docker build -t jewel-erp:latest .
```

## 📚 API Documentation

Once running, access the API documentation at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## 🏗️ Project Structure

```
src/main/java/com/aurajewels/jewel/
├── common/              # Shared infrastructure
│   ├── config/          # App configurations
│   ├── entity/          # Base entities
│   └── exception/       # Exception handling
├── inventory/           # Inventory management module
├── billing/             # Invoicing & billing module
├── stock/               # Stock management module
├── customer/            # Customer management
├── supplier/            # Supplier management
└── security/            # Authentication & authorization
```

## 🔧 Configuration

Key environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/jewelerpdb` |
| `DATABASE_USERNAME` | Database username | `jeweluser` |
| `DATABASE_PASSWORD` | Database password | `jewelpass` |
| `JWT_SECRET` | JWT signing key (256-bit) | - |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |

## 🐳 Deployment

### Kubernetes (EKS)

```bash
# Apply configurations
kubectl apply -f deploy/namespace.yaml
kubectl apply -f deploy/configmap.yaml
kubectl apply -f deploy/secret.yaml
kubectl apply -f deploy/deployment.yaml
kubectl apply -f deploy/service.yaml
kubectl apply -f deploy/ingress.yaml
```

### Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Metrics**: `/actuator/prometheus`

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw verify

# Coverage report at: target/site/jacoco/index.html
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.
