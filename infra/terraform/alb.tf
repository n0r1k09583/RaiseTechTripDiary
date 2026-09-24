resource "aws_lb" "api" {
  count              = local.on
  name               = "${var.name}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb[0].id]
  subnets            = aws_subnet.public[*].id
}

resource "aws_lb_target_group" "api" {
  count       = local.on
  name        = "${var.name}-api"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main[0].id
  target_type = "ip"
  health_check {
    path                = "/api/health"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
  }
}

resource "aws_lb_listener" "http" {
  count             = local.on
  load_balancer_arn = aws_lb.api[0].arn
  port              = 80
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api[0].arn
  }
}
