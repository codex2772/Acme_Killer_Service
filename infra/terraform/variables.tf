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
  default     = 1024
}

variable "container_memory" {
  # 2 GB: headless Chromium (branded invoice PDF render) alongside the JVM does
  # not fit in 1 GB and will OOM the Fargate task.
  description = "Container memory in MB"
  type        = number
  default     = 2048
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

# ================================
# Meta WhatsApp
# ================================
variable "meta_app_secret" {
  description = "Meta app secret (App Settings > Basic); verifies WhatsApp webhook signatures. Blank = signature verification skipped."
  type        = string
  sensitive   = true
  default     = ""
}

variable "meta_webhook_verify_token" {
  description = "Self-chosen token echoed during the Meta webhook verification handshake"
  type        = string
  sensitive   = true
  default     = ""
}

variable "waba_token_store" {
  description = "Per-store WhatsApp token storage: 'local' (AES-encrypted in DB) or 'secrets-manager'"
  type        = string
  default     = "local"

  validation {
    condition     = contains(["local", "secrets-manager"], var.waba_token_store)
    error_message = "waba_token_store must be 'local' or 'secrets-manager'."
  }
}

variable "waba_secret_prefix" {
  description = "Secrets Manager name prefix for per-store WhatsApp tokens (used when waba_token_store = secrets-manager)"
  type        = string
  default     = "jewel-erp/prod/waba"
}

