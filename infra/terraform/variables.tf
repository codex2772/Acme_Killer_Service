# ================================
# General
# ================================
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "prod"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "jewel-erp"
}

# ================================
# Networking
# ================================
variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones"
  type        = list(string)
  default     = ["ap-south-1a", "ap-south-1b"]
}

# ================================
# ECS
# ================================
variable "container_port" {
  description = "Container port"
  type        = number
  default     = 8080
}

variable "container_cpu" {
  description = "Container CPU units"
  type        = number
  default     = 512
}

variable "container_memory" {
  description = "Container memory in MB"
  type        = number
  default     = 1024
}

variable "desired_count" {
  description = "Desired number of tasks"
  type        = number
  default     = 1
}

# ================================
# RDS MySQL
# ================================
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "jewelerpdb"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "jeweladmin"
  sensitive   = true
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}

# ================================
# JWT
# ================================
variable "jwt_secret" {
  description = "JWT signing secret key (min 32 chars)"
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must be at least 32 characters: JwtUtil uses HS256, which rejects keys under 256 bits with a WeakKeyException at startup."
  }
}

# ================================
# Razorpay
# ================================
variable "razorpay_key_id" {
  description = "Razorpay API key ID (from Razorpay Dashboard > Settings > API Keys)"
  type        = string
  sensitive   = true
}

variable "razorpay_key_secret" {
  description = "Razorpay API key secret (shown only once at generation)"
  type        = string
  sensitive   = true
}

variable "razorpay_webhook_secret" {
  description = "Razorpay webhook signing secret (self-chosen; must match the Razorpay Dashboard webhook config)"
  type        = string
  sensitive   = true
}

