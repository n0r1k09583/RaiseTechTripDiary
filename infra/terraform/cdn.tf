data "aws_caller_identity" "current" {
  count = local.on
}

resource "aws_s3_bucket" "web" {
  count         = local.on
  bucket        = "${var.name}-web-${data.aws_caller_identity.current[0].account_id}"
  force_destroy = true
}

resource "aws_s3_bucket" "images" {
  count         = local.on
  bucket        = "${var.name}-images-${data.aws_caller_identity.current[0].account_id}"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "web" {
  count                   = local.on
  bucket                  = aws_s3_bucket.web[0].id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "images" {
  count                   = local.on
  bucket                  = aws_s3_bucket.images[0].id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_cloudfront_origin_access_control" "s3" {
  count                             = local.on
  name                              = "${var.name}-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

data "aws_cloudfront_cache_policy" "caching_disabled" {
  count = local.on
  name  = "Managed-CachingDisabled"
}

data "aws_cloudfront_origin_request_policy" "all_viewer_except_host" {
  count = local.on
  name  = "Managed-AllViewerExceptHostHeader"
}

resource "aws_cloudfront_distribution" "cdn" {
  count               = local.on
  enabled             = true
  default_root_object = "index.html"

  origin {
    domain_name              = aws_s3_bucket.web[0].bucket_regional_domain_name
    origin_id                = "web"
    origin_access_control_id = aws_cloudfront_origin_access_control.s3[0].id
  }

  origin {
    domain_name              = aws_s3_bucket.images[0].bucket_regional_domain_name
    origin_id                = "images"
    origin_access_control_id = aws_cloudfront_origin_access_control.s3[0].id
  }

  origin {
    domain_name = aws_lb.api[0].dns_name
    origin_id   = "api"
    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  default_cache_behavior {
    target_origin_id       = "web"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    forwarded_values {
      query_string = true
      cookies {
        forward = "none"
      }
    }
  }

  ordered_cache_behavior {
    path_pattern             = "/api/*"
    target_origin_id         = "api"
    viewer_protocol_policy   = "redirect-to-https"
    allowed_methods          = ["GET", "HEAD", "OPTIONS", "PUT", "POST", "PATCH", "DELETE"]
    cached_methods           = ["GET", "HEAD"]
    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled[0].id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer_except_host[0].id
  }

  ordered_cache_behavior {
    path_pattern           = "/uploads/*"
    target_origin_id       = "images"
    viewer_protocol_policy = "redirect-to-https"
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
  }

  custom_error_response {
    error_code         = 403
    response_code      = 200
    response_page_path = "/index.html"
  }

  custom_error_response {
    error_code         = 404
    response_code      = 200
    response_page_path = "/index.html"
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

data "aws_iam_policy_document" "web_oac" {
  count = local.on
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.web[0].arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.cdn[0].arn]
    }
  }
}

data "aws_iam_policy_document" "images_oac" {
  count = local.on
  statement {
    actions   = ["s3:GetObject"]
    resources = ["${aws_s3_bucket.images[0].arn}/*"]
    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.cdn[0].arn]
    }
  }
}

resource "aws_s3_bucket_policy" "web" {
  count  = local.on
  bucket = aws_s3_bucket.web[0].id
  policy = data.aws_iam_policy_document.web_oac[0].json
}

resource "aws_s3_bucket_policy" "images" {
  count  = local.on
  bucket = aws_s3_bucket.images[0].id
  policy = data.aws_iam_policy_document.images_oac[0].json
}
