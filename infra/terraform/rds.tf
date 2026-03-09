# ================================
# RDS Security Group
# ================================
resource "aws_security_group" "rds" {
  name        = "${var.app_name}-rds-sg"
  description = "Security group for RDS MySQL"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "MySQL from ECS tasks"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.app_name}-rds-sg"
  }
}

# ================================
# RDS Subnet Group (private subnets)
# ================================
resource "aws_db_subnet_group" "main" {
  name       = "${var.app_name}-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = {
    Name = "${var.app_name}-db-subnet-group"
  }
}

# ================================
# Secrets Manager (DB credentials)
# ================================
resource "aws_secretsmanager_secret" "db_credentials" {
  name                    = "${var.app_name}/${var.environment}/db-credentials"
  description             = "RDS MySQL credentials for ${var.app_name}"
  recovery_window_in_days = 7

  tags = {
    Name = "${var.app_name}-db-credentials"
  }
}

resource "aws_secretsmanager_secret_version" "db_credentials" {
  secret_id = aws_secretsmanager_secret.db_credentials.id
  secret_string = jsonencode({
    username   = var.db_username
    password   = var.db_password
    host       = aws_db_instance.main.address
    port       = 3306
    dbname     = var.db_name
    url        = "jdbc:mysql://${aws_db_instance.main.address}:3306/${var.db_name}?useSSL=true&requireSSL=true"
    jwt_secret = var.jwt_secret
  })

  lifecycle {
    ignore_changes = [secret_string]
  }
}

# ================================
# RDS MySQL Instance
# ================================
resource "aws_db_instance" "main" {
  identifier = "${var.app_name}-db"

  engine         = "mysql"
  engine_version = "8.0"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 50
  storage_type          = "gp3"
  storage_encrypted     = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  multi_az            = false
  publicly_accessible = false
  skip_final_snapshot = true

  backup_retention_period = 1
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  parameter_group_name = aws_db_parameter_group.main.name

  tags = {
    Name = "${var.app_name}-db"
  }
}

# ================================
# DB Parameter Group
# ================================
resource "aws_db_parameter_group" "main" {
  name   = "${var.app_name}-db-params"
  family = "mysql8.0"

  parameter {
    name  = "character_set_server"
    value = "utf8mb4"
  }

  parameter {
    name  = "collation_server"
    value = "utf8mb4_unicode_ci"
  }

  tags = {
    Name = "${var.app_name}-db-params"
  }
}
