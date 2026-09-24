resource "random_password" "db" {
  count   = local.on
  length  = 20
  special = false
}

resource "aws_db_subnet_group" "main" {
  count      = local.on
  name       = "${var.name}-db"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_db_instance" "postgres" {
  count                        = local.on
  identifier                   = "${var.name}-pg"
  engine                       = "postgres"
  engine_version               = "17"
  instance_class               = "db.t3.micro"
  allocated_storage            = 20
  db_name                      = "timeline"
  username                     = "timeline"
  password                     = random_password.db[0].result
  db_subnet_group_name         = aws_db_subnet_group.main[0].name
  vpc_security_group_ids       = [aws_security_group.rds[0].id]
  publicly_accessible          = false
  multi_az                     = false
  skip_final_snapshot          = true
  backup_retention_period      = 0
  deletion_protection          = false
  performance_insights_enabled = false
}
