terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Bucket name must be a literal: backend blocks cannot reference variables or
  # data sources. Account-ID suffix keeps it globally unique.
  backend "s3" {
    bucket         = "jewel-erp-tfstate-950639281869"
    key            = "ecs/terraform.tfstate"
    region         = "ap-south-1"
    encrypt        = true
    dynamodb_table = "terraform-locks"
  }
}

data "aws_caller_identity" "current" {}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "JewelERP"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}
