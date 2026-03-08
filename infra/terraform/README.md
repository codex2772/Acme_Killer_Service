# Terraform Infrastructure for JewelERP

This directory contains Terraform configurations to provision AWS infrastructure for JewelERP ECS deployment.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                           AWS Cloud                             │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                         VPC                                │  │
│  │  ┌─────────────────┐       ┌─────────────────┐           │  │
│  │  │  Public Subnet  │       │  Public Subnet  │           │  │
│  │  │   (AZ-1a)       │       │   (AZ-1b)       │           │  │
│  │  │  ┌───────────┐  │       │                 │           │  │
│  │  │  │    ALB    │──┼───────┼─────────────────┼───► Internet│
│  │  │  └─────┬─────┘  │       │                 │           │  │
│  │  └────────┼────────┘       └─────────────────┘           │  │
│  │           │                                               │  │
│  │  ┌────────▼────────┐       ┌─────────────────┐           │  │
│  │  │ Private Subnet  │       │ Private Subnet  │           │  │
│  │  │   (AZ-1a)       │       │   (AZ-1b)       │           │  │
│  │  │  ┌───────────┐  │       │  ┌───────────┐  │           │  │
│  │  │  │ ECS Task  │  │       │  │ ECS Task  │  │           │  │
│  │  │  │ (Fargate) │  │       │  │ (Fargate) │  │           │  │
│  │  │  └───────────┘  │       │  └───────────┘  │           │  │
│  │  └─────────────────┘       └─────────────────┘           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │
│  │     ECR     │  │ CloudWatch  │  │   Secrets Manager       │ │
│  │  (Images)   │  │   (Logs)    │  │   (DB creds, JWT)       │ │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Resources Created

| Resource | Description |
|----------|-------------|
| VPC | Virtual Private Cloud with DNS support |
| Subnets | 2 public + 2 private subnets across 2 AZs |
| Internet Gateway | For public internet access |
| NAT Gateway | For private subnet internet access |
| ALB | Application Load Balancer |
| Target Group | Health check on `/actuator/health/readiness` |
| ECR | Container registry for Docker images |
| ECS Cluster | Fargate cluster |
| ECS Service | Service with 2 tasks |
| Task Definition | Container configuration |
| CloudWatch Logs | Log group with 30-day retention |
| Secrets Manager | Secure storage for DB credentials |
| Auto Scaling | CPU/Memory based scaling (1-4 tasks) |
| IAM Roles | Task execution and task roles |
| Security Groups | ALB and ECS tasks |

## Prerequisites

1. [Terraform](https://www.terraform.io/downloads.html) >= 1.0
2. [AWS CLI](https://aws.amazon.com/cli/) configured with credentials
3. AWS account with appropriate permissions

## Quick Start

```bash
# 1. Navigate to terraform directory
cd infra/terraform

# 2. Copy and edit variables
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values

# 3. Initialize Terraform
terraform init

# 4. Preview changes
terraform plan

# 5. Apply infrastructure
terraform apply

# 6. Note the outputs (ECR URL, ALB DNS, etc.)
terraform output
```

## After Infrastructure is Created

### 1. Update Secrets Manager
```bash
# Update with real credentials
aws secretsmanager update-secret \
  --secret-id jewel-erp/prod/secrets \
  --secret-string '{
    "DATABASE_URL": "jdbc:postgresql://your-rds:5432/jewelerpdb",
    "DATABASE_USERNAME": "your-user",
    "DATABASE_PASSWORD": "your-password",
    "JWT_SECRET": "your-256-bit-secret"
  }'
```

### 2. Push Docker Image
```bash
# Login to ECR
aws ecr get-login-password --region ap-south-1 | \
  docker login --username AWS --password-stdin $(terraform output -raw ecr_repository_url | cut -d/ -f1)

# Build and push
docker build -t jewel-erp:latest ../../
docker tag jewel-erp:latest $(terraform output -raw ecr_repository_url):latest
docker push $(terraform output -raw ecr_repository_url):latest

# Force new deployment
aws ecs update-service \
  --cluster $(terraform output -raw ecs_cluster_name) \
  --service $(terraform output -raw ecs_service_name) \
  --force-new-deployment
```

### 3. Access Your Application
```bash
# Get ALB URL
echo "http://$(terraform output -raw alb_dns_name)/api/hello"
```

## GitHub Actions Setup

### Infrastructure Pipeline (Recommended)

A dedicated infrastructure pipeline is available at **Actions → Infrastructure**.

**How to use:**
1. Go to **Actions** tab in GitHub
2. Select **Infrastructure** workflow
3. Click **Run workflow**
4. Choose action: `plan`, `apply`, or `destroy`
5. Choose environment: `prod` or `staging`
6. Click **Run workflow**

```
┌─────────────────────────────────────────────────┐
│         GitHub Actions - Infrastructure         │
├─────────────────────────────────────────────────┤
│  Action:      [plan ▼]                          │
│               ┌──────────┐                      │
│               │ plan     │  ← Preview changes   │
│               │ apply    │  ← Create resources  │
│               │ destroy  │  ← Delete all        │
│               └──────────┘                      │
│  Environment: [prod ▼]                          │
│                                                 │
│  [Run workflow]                                 │
└─────────────────────────────────────────────────┘
```

### Required Secrets

Add these secrets to your GitHub repository:

| Secret | Value |
|--------|-------|
| `AWS_ACCESS_KEY_ID` | Your AWS access key |
| `AWS_SECRET_ACCESS_KEY` | Your AWS secret key |

### Required Variables

Add these variables:

| Variable | Value |
|----------|-------|
| `AWS_REGION` | `ap-south-1` |

## Cost Estimate (ap-south-1)

| Resource | Estimated Monthly Cost |
|----------|------------------------|
| NAT Gateway | ~$32 |
| ALB | ~$16 |
| Fargate (2 tasks, 0.5vCPU, 1GB) | ~$30 |
| CloudWatch Logs | ~$2 |
| Secrets Manager | ~$1 |
| **Total** | **~$80/month** |

> 💡 **Cost Saving Tips:**
> - Use `FARGATE_SPOT` for non-production
> - Scale down to 1 task during off-hours
> - Consider NAT Instance instead of NAT Gateway (~$5/month)

## Cleanup

```bash
# Destroy all resources
terraform destroy
```

⚠️ **Warning:** This will delete all resources including data. Make sure to backup any important data first.
