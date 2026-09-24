resource "aws_ecr_repository" "api" {
  count                = local.on
  name                 = "${var.name}-api"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

resource "aws_cloudwatch_log_group" "api" {
  count             = local.on
  name              = "/ecs/${var.name}"
  retention_in_days = 7
}

resource "aws_iam_role" "exec" {
  count = local.on
  name  = "${var.name}-ecs-exec"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "exec" {
  count      = local.on
  role       = aws_iam_role.exec[0].name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task" {
  count = local.on
  name  = "${var.name}-ecs-task"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "images" {
  count = local.on
  name  = "${var.name}-s3-images"
  role  = aws_iam_role.task[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["s3:PutObject", "s3:DeleteObject", "s3:GetObject"]
      Resource = ["${aws_s3_bucket.images[0].arn}/*"]
    }]
  })
}

resource "aws_ecs_cluster" "main" {
  count = local.on
  name  = var.name
}

resource "aws_ecs_task_definition" "api" {
  count                    = local.on
  family                   = "${var.name}-api"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.exec[0].arn
  task_role_arn            = aws_iam_role.task[0].arn
  container_definitions = jsonencode([{
    name      = "api"
    image     = "${aws_ecr_repository.api[0].repository_url}:latest"
    essential = true
    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "aws" },
      { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${aws_db_instance.postgres[0].address}:5432/timeline" },
      { name = "SPRING_DATASOURCE_USERNAME", value = aws_db_instance.postgres[0].username },
      { name = "SPRING_DATASOURCE_PASSWORD", value = random_password.db[0].result },
      { name = "JWT_SECRET", value = var.jwt_secret },
      { name = "AWS_S3_BUCKET", value = aws_s3_bucket.images[0].bucket },
      { name = "AWS_S3_REGION", value = var.aws_region },
      { name = "CORS_ALLOWED_ORIGINS", value = "https://${aws_cloudfront_distribution.cdn[0].domain_name}" },
      { name = "APP_STORAGE", value = "s3" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.api[0].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "api"
      }
    }
  }])
}

resource "aws_ecs_service" "api" {
  count           = local.on
  name            = "${var.name}-api"
  cluster         = aws_ecs_cluster.main[0].id
  task_definition = aws_ecs_task_definition.api[0].arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"
  network_configuration {
    subnets          = aws_subnet.public[*].id
    security_groups  = [aws_security_group.ecs[0].id]
    assign_public_ip = true
  }
  load_balancer {
    target_group_arn = aws_lb_target_group.api[0].arn
    container_name   = "api"
    container_port   = 8080
  }
  depends_on = [aws_lb_listener.http]
}
