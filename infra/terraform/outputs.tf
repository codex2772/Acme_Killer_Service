# ================================
# VPC Outputs
# ================================
output "vpc_id" {
  description = "VPC ID"
  value       = aws_vpc.main.id
}

output "public_subnets" {
  description = "Public subnet IDs"
  value       = aws_subnet.public[*].id
}

output "private_subnets" {
  description = "Private subnet IDs"
  value       = aws_subnet.private[*].id
}

# ================================
# ECR Outputs
# ================================
output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.main.repository_url
}

output "ecr_repository_name" {
  description = "ECR repository name"
  value       = aws_ecr_repository.main.name
}

# ================================
# ECS Outputs
# ================================
output "ecs_cluster_name" {
  description = "ECS cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS service name"
  value       = aws_ecs_service.main.name
}

# ================================
# ALB Outputs
# ================================
output "alb_dns_name" {
  description = "ALB DNS name (your app URL)"
  value       = aws_lb.main.dns_name
}

output "alb_zone_id" {
  description = "ALB zone ID (for Route53)"
  value       = aws_lb.main.zone_id
}

# ================================
# RDS Outputs
# ================================
output "rds_endpoint" {
  description = "RDS MySQL endpoint"
  value       = aws_db_instance.main.address
}

output "rds_port" {
  description = "RDS MySQL port"
  value       = aws_db_instance.main.port
}

output "db_credentials_secret_arn" {
  description = "Secrets Manager ARN for DB credentials"
  value       = aws_secretsmanager_secret.db_credentials.arn
}

# ================================
# S3 Outputs
# ================================
output "s3_images_bucket" {
  description = "S3 bucket name for images"
  value       = aws_s3_bucket.images.bucket
}

output "s3_images_bucket_url" {
  description = "S3 bucket URL for images"
  value       = "https://${aws_s3_bucket.images.bucket}.s3.${var.aws_region}.amazonaws.com"
}

