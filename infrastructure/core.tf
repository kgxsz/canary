# Variables
variable "project" { default = "canary" }
variable "AWS_REGION" { default = "eu-west-1" }

# Provider
provider "aws" {
  region = "${var.AWS_REGION}"
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
      "Resource": "arn:aws:logs:${var.AWS_REGION}:*:log-group:/aws/lambda/${var.project}:*"
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
      "Resource": "arn:aws:dynamodb:${var.AWS_REGION}:*:table/${var.project}"
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
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  parent_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part   = "query"
}

resource "aws_api_gateway_method" "query" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.query.id}"
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_resource" "command" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  parent_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part   = "command"
}

resource "aws_api_gateway_method" "command" {
  rest_api_id   = "${aws_api_gateway_rest_api.api.id}"
  resource_id   = "${aws_api_gateway_resource.command.id}"
  http_method   = "POST"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "lambda_query_integration" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_method.query.resource_id}"
  http_method = "${aws_api_gateway_method.query.http_method}"
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${aws_lambda_function.lambda.invoke_arn}"
}

resource "aws_api_gateway_integration" "lambda_command_integration" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_method.command.resource_id}"
  http_method = "${aws_api_gateway_method.command.http_method}"
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = "${aws_lambda_function.lambda.invoke_arn}"
}

resource "aws_api_gateway_deployment" "api_deployment" {
  depends_on = [
    "aws_api_gateway_integration.lambda_query_integration",
    "aws_api_gateway_integration.lambda_command_integration",
  ]
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name = "default"
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
