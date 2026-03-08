# ECS Deployment Guide

## Prerequisites

1. **AWS CLI** configured with appropriate credentials
2. **ECR Repository** created for the Docker image
3. **ECS Cluster** (Fargate) created
4. **VPC** with private subnets and NAT Gateway
5. **Application Load Balancer** with target group
6. **AWS Secrets Manager** secret for sensitive config

## Infrastructure Setup

### 1. Create ECR Repository
```bash
aws ecr create-repository \
  --repository-name jewel-erp \
  --region $AWS_REGION
```

### 2. Create ECS Cluster
```bash
aws ecs create-cluster \
  --cluster-name jewel-erp-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy capacityProvider=FARGATE,weight=1
```

### 3. Create CloudWatch Log Group
```bash
aws logs create-log-group \
  --log-group-name /ecs/jewel-erp \
  --region $AWS_REGION
```

### 4. Create Secrets in AWS Secrets Manager
```bash
aws secretsmanager create-secret \
  --name jewel-erp/prod \
  --secret-string '{
    "DATABASE_URL": "jdbc:postgresql://your-rds:5432/jewelerpdb",
    "DATABASE_USERNAME": "your-username",
    "DATABASE_PASSWORD": "your-password",
    "JWT_SECRET": "your-256-bit-secret"
  }'
```

### 5. Create IAM Roles

**Task Execution Role** (for pulling images, secrets):
```bash
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file://ecs-trust-policy.json

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy
```

**Task Role** (for application access to AWS services):
```bash
aws iam create-role \
  --role-name jewelErpTaskRole \
  --assume-role-policy-document file://ecs-trust-policy.json
```

## Deployment Steps

### 1. Build and Push Docker Image
```bash
# Login to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin $ECR_REGISTRY

# Build image
docker build -t jewel-erp:latest .

# Tag and push
docker tag jewel-erp:latest $ECR_REGISTRY/jewel-erp:latest
docker push $ECR_REGISTRY/jewel-erp:latest
```

### 2. Register Task Definition
```bash
# Replace variables in task-definition.json first
aws ecs register-task-definition \
  --cli-input-json file://deploy/ecs/task-definition.json
```

### 3. Create/Update Service
```bash
# Create service (first time)
aws ecs create-service \
  --cli-input-json file://deploy/ecs/service-definition.json

# Update service (subsequent deployments)
aws ecs update-service \
  --cluster jewel-erp-cluster \
  --service jewel-erp-service \
  --force-new-deployment
```

### 4. Verify Deployment
```bash
# Check service status
aws ecs describe-services \
  --cluster jewel-erp-cluster \
  --services jewel-erp-service

# Check running tasks
aws ecs list-tasks \
  --cluster jewel-erp-cluster \
  --service-name jewel-erp-service
```

## Health Check Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health/liveness` | Container health |
| `/actuator/health/readiness` | Service readiness |
| `/api/hello` | Application test |

## Environment Variables

| Variable | Source | Description |
|----------|--------|-------------|
| `SPRING_PROFILES_ACTIVE` | Task Definition | Active profile (prod) |
| `PORT` | Task Definition | Server port (8080) |
| `DATABASE_URL` | Secrets Manager | PostgreSQL connection |
| `DATABASE_USERNAME` | Secrets Manager | DB username |
| `DATABASE_PASSWORD` | Secrets Manager | DB password |
| `JWT_SECRET` | Secrets Manager | JWT signing key |

## Scaling

Auto-scaling is configured based on CPU/Memory utilization. Modify `autoscaling.json` and apply:

```bash
aws application-autoscaling register-scalable-target \
  --service-namespace ecs \
  --resource-id service/jewel-erp-cluster/jewel-erp-service \
  --scalable-dimension ecs:service:DesiredCount \
  --min-capacity 2 \
  --max-capacity 10
```
