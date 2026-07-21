# ================================
# S3 Bucket for image uploads
# ================================
resource "aws_s3_bucket" "images" {
  # S3 bucket names are globally unique across all AWS accounts. The account-ID
  # suffix guarantees this name can never collide with another account's.
  bucket = "${var.app_name}-images-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name = "${var.app_name}-images"
  }
}

resource "aws_s3_bucket_public_access_block" "images" {
  bucket = aws_s3_bucket.images.id

  block_public_acls       = false
  block_public_policy     = false
  ignore_public_acls      = false
  restrict_public_buckets = false
}

resource "aws_s3_bucket_policy" "images_public_read" {
  bucket = aws_s3_bucket.images.id

  depends_on = [aws_s3_bucket_public_access_block.images]

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "PublicReadGetObject"
        Effect    = "Allow"
        Principal = "*"
        Action    = "s3:GetObject"
        Resource  = "${aws_s3_bucket.images.arn}/*"
      }
    ]
  })
}

resource "aws_s3_bucket_cors_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "PUT", "POST"]
    allowed_origins = ["*"]
    max_age_seconds = 3600
  }
}

# Invoice PDFs are hosted publicly only so Meta can fetch them for WhatsApp
# document delivery. Meta caches the file at send time, so the object is
# short-lived — expire it after 1 day to limit exposure of customer bills.
resource "aws_s3_bucket_lifecycle_configuration" "images" {
  bucket = aws_s3_bucket.images.id

  rule {
    id     = "expire-invoice-pdfs"
    status = "Enabled"

    filter {
      prefix = "invoices/"
    }

    expiration {
      days = 1
    }
  }
}

# ================================
# IAM Policy — ECS task role can upload to S3
# ================================
resource "aws_iam_role_policy" "ecs_task_s3" {
  name = "${var.app_name}-ecs-s3-policy"
  role = aws_iam_role.ecs_task_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.images.arn,
          "${aws_s3_bucket.images.arn}/*"
        ]
      }
    ]
  })
}
