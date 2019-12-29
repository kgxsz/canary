# Variables
variable "project" { default = "canary" }
variable "aws_region" { default = "eu-west-1" }
variable "authorisation_client_id" { default = "db25cb77a169c3b6565f" }
variable "authorisation_client_secret" {}
variable "cookie_store_key" {}
variable "cookie_attribute_domain" { default = ".kaizen.keigo.io" }
variable "cookie_attribute_secure" { default = "true" }

# Provider
provider "aws" {
  region = "${var.aws_region}"
}

# S3
resource "aws_s3_bucket" "bucket" {
  bucket = "api.${var.project}.keigo.io"
  acl = "private"
}

resource "aws_s3_bucket_object" "uberjar" {
  bucket = "api.${var.project}.keigo.io"
  depends_on = ["aws_s3_bucket.bucket"]
  key = "${var.project}.jar"
  source = "target/${var.project}.jar"
  etag = "${filemd5("target/${var.project}.jar")}"
}

# Lambda
resource "aws_iam_policy" "lambda_policy" {
  name        = "${var.project}"
  path        = "/"
  description = "Lambda execution policy"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:logs:${var.aws_region}:*:log-group:/aws/lambda/${var.project}:*"
    },
    {
      "Action": [
        "dynamodb:DescribeTable",
        "dynamodb:Query",
        "dynamodb:Scan",
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:BatchWriteItem",
        "dynamodb:DeleteItem"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:dynamodb:${var.aws_region}:*:table/${var.project}"
    }
  ]
}
EOF
}

resource "aws_iam_role" "lambda_role" {
  name = "${var.project}"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": "1"
    }
  ]
}
EOF
}


resource "aws_iam_policy_attachment" "lambda_policy_attachment" {
  name = "${var.project}"
  roles = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "${aws_iam_policy.lambda_policy.arn}"
}


resource "aws_lambda_function" "lambda" {
  depends_on = ["aws_s3_bucket.bucket",
                "aws_iam_role.lambda_role",
                "aws_s3_bucket_object.uberjar",
                "aws_iam_policy.lambda_policy"]
  s3_bucket          = "${aws_s3_bucket.bucket.bucket}"
  s3_key             = "${var.project}.jar"
  function_name      = "${var.project}"
  description        = "API for ${var.project}.keigo.io"
  role               = "${aws_iam_role.lambda_role.arn}"
  handler            = "${var.project}.Handler"
  source_code_hash   = "${base64sha256("target/${var.project}.jar")}"
  runtime            = "java8"
  timeout            = 100
  memory_size        = 512
  environment {
    variables = {
      AUTHORISATION_CLIENT_ID = var.authorisation_client_id
      AUTHORISATION_CLIENT_SECRET = var.authorisation_client_secret
      COOKIE_STORE_KEY = var.cookie_store_key
      COOKIE_ATTRIBUTE_DOMAIN = var.cookie_attribute_domain
      COOKIE_ATTRIBUTE_SECURE = var.cookie_attribute_secure
    }
  }
}


resource "aws_lambda_permission" "api_gateway_permission" {
  function_name = "${aws_lambda_function.lambda.arn}"
  action = "lambda:InvokeFunction"
  statement_id = "AllowExecutionFromApiGateway"
  principal = "apigateway.amazonaws.com"
}

# API Gateway
resource "aws_api_gateway_rest_api" "api" {
  name = "${var.project}"
}

resource "aws_api_gateway_resource" "query" {
  path_part   = "query"
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  parent_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
}

resource "aws_api_gateway_resource" "command" {
  path_part   = "command"
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  parent_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
}

resource "aws_api_gateway_method" "query_options_method" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "OPTIONS"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "command_options_method" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "OPTIONS"
  authorization = "NONE"
}

resource "aws_api_gateway_method_response" "query_options_method_response" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "${aws_api_gateway_method.query_options_method.http_method}"
  status_code   = "200"
  response_models = {
    "application/json" = "Empty"
  }
  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = true,
    "method.response.header.Access-Control-Expose-Headers" = true,
    "method.response.header.Access-Control-Allow-Methods" = true,
    "method.response.header.Access-Control-Allow-Credentials" = true,
    "method.response.header.Access-Control-Allow-Origin" = true
  }
  depends_on = ["aws_api_gateway_method.query_options_method"]
}

resource "aws_api_gateway_method_response" "command_options_method_response" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "${aws_api_gateway_method.command_options_method.http_method}"
  status_code   = "200"
  response_models = {
    "application/json" = "Empty"
  }
  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = true,
    "method.response.header.Access-Control-Expose-Headers" = true,
    "method.response.header.Access-Control-Allow-Methods" = true,
    "method.response.header.Access-Control-Allow-Credentials" = true,
    "method.response.header.Access-Control-Allow-Origin" = true
  }
  depends_on = ["aws_api_gateway_method.command_options_method"]
}

resource "aws_api_gateway_integration" "query_options_integration" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "${aws_api_gateway_method.query_options_method.http_method}"
  type          = "MOCK"
  depends_on    = ["aws_api_gateway_method.query_options_method"]
  request_templates = {
    "application/json" = jsonencode({statusCode = 200})
  }
}

resource "aws_api_gateway_integration" "command_options_integration" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "${aws_api_gateway_method.command_options_method.http_method}"
  type          = "MOCK"
  depends_on    = ["aws_api_gateway_method.command_options_method"]
  request_templates = {
    "application/json" = jsonencode({statusCode = 200})
  }
}

resource "aws_api_gateway_integration_response" "query_options_integration_response" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "${aws_api_gateway_method.query_options_method.http_method}"
  status_code   = "${aws_api_gateway_method_response.query_options_method_response.status_code}"
  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
    "method.response.header.Access-Control-Allow-Methods" = "'OPTIONS,POST'"
    "method.response.header.Access-Control-Allow-Credentials" = "'true'"
    "method.response.header.Access-Control-Allow-Origin" = "'https://kaizen.keigo.io'"
  }
  response_templates = {
    "application/json" = ""
  }
  depends_on = ["aws_api_gateway_method_response.query_options_method_response"]
}

resource "aws_api_gateway_integration_response" "command_options_integration_response" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "${aws_api_gateway_method.command_options_method.http_method}"
  status_code   = "${aws_api_gateway_method_response.command_options_method_response.status_code}"
  response_parameters = {
    "method.response.header.Access-Control-Allow-Headers" = "'Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token'"
    "method.response.header.Access-Control-Allow-Methods" = "'OPTIONS,POST'"
    "method.response.header.Access-Control-Allow-Credentials" = "'true'"
    "method.response.header.Access-Control-Allow-Origin" = "'https://kaizen.keigo.io'"
  }
  response_templates = {
    "application/json" = ""
  }
  depends_on = ["aws_api_gateway_method_response.command_options_method_response"]
}

resource "aws_api_gateway_method" "query_post_method" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method" "command_post_method" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_method_response" "query_post_method_response" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "${aws_api_gateway_method.query_post_method.http_method}"
  status_code   = "200"
  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin" = false
    "method.response.header.Access-Control-Allow-Credentials" = false
    "method.response.header.Access-Control-Allow-Headers" = false
    "method.response.header.Access-Control-Allow-Methods" = false
  }
  depends_on = ["aws_api_gateway_method.query_post_method"]
}

resource "aws_api_gateway_method_response" "command_post_method_response" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "${aws_api_gateway_method.command_post_method.http_method}"
  status_code   = "200"
  response_parameters = {
    "method.response.header.Access-Control-Allow-Origin" = false
    "method.response.header.Access-Control-Allow-Credentials" = false
    "method.response.header.Access-Control-Allow-Headers" = false
    "method.response.header.Access-Control-Allow-Methods" = false

  }
  depends_on = ["aws_api_gateway_method.command_post_method"]
}

resource "aws_api_gateway_integration" "query_post_integration" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_resource.query.id}"
  http_method = "${aws_api_gateway_method.query_post_method.http_method}"
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = "${aws_lambda_function.lambda.invoke_arn}"
  depends_on              = ["aws_api_gateway_method.query_post_method", "aws_lambda_function.lambda"]
}

resource "aws_api_gateway_integration" "command_post_integration" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_resource.command.id}"
  http_method = "${aws_api_gateway_method.command_post_method.http_method}"
  type                    = "AWS_PROXY"
  integration_http_method = "POST"
  uri                     = "${aws_lambda_function.lambda.invoke_arn}"
  depends_on              = ["aws_api_gateway_method.command_post_method", "aws_lambda_function.lambda"]
}

resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name = "default"
  depends_on = ["aws_api_gateway_integration.query_post_integration", "aws_api_gateway_integration.command_post_integration"]
}

resource "aws_api_gateway_base_path_mapping" "mapping" {
  api_id      = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "${aws_api_gateway_deployment.api_deployment.stage_name}"
  domain_name =  "api.kaizen.keigo.io"
}

# DynamoDB
resource "aws_dynamodb_table" "table" {
  name           = "${var.project}"
  billing_mode   = "PROVISIONED"
  read_capacity  = 1
  write_capacity = 1
  hash_key       = "partition"
  range_key      = "sort"

  attribute {
    name = "partition"
    type = "S"
  }

  attribute {
    name = "sort"
    type = "N"
  }
}
