terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

variable "aws_region" {
  type    = string
  default = "ap-northeast-1"
}

variable "name" {
  type    = string
  default = "tripdiary"
}

variable "enable_infra" {
  type        = bool
  default     = false
  description = "true のときだけ AWS に作る。既定 false。確認が終わったら destroy。"
}

variable "desired_count" {
  type        = number
  default     = 0
  description = "Fargate の台数。確認するときだけ 1。普段は 0。"
}

variable "jwt_secret" {
  type      = string
  sensitive = true
  default   = "change-me-before-any-apply"
}

locals {
  on = var.enable_infra ? 1 : 0
  az = var.enable_infra ? 2 : 0
}
