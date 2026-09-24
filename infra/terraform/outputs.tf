output "cloudfront_url" {
  value = local.on == 1 ? "https://${aws_cloudfront_distribution.cdn[0].domain_name}" : "disabled (enable_infra=false)"
}

output "alb_dns" {
  value = local.on == 1 ? aws_lb.api[0].dns_name : "disabled"
}

output "ecr_url" {
  value = local.on == 1 ? aws_ecr_repository.api[0].repository_url : "disabled"
}

output "web_bucket" {
  value = local.on == 1 ? aws_s3_bucket.web[0].bucket : "disabled"
}

output "images_bucket" {
  value = local.on == 1 ? aws_s3_bucket.images[0].bucket : "disabled"
}
