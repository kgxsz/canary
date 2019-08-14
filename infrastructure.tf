variable "project" { default = "canary" }
variable "AWS_REGION" { default = "eu-west-1" }

provider "aws" {
  region = "${var.AWS_REGION}"
}

resource "aws_s3_bucket" "uberjar_bucket" {
  bucket = "api.${var.project}.keigo.io"
  acl = "private"
}

resource "aws_s3_bucket_object" "uberjar_file" {
  bucket = "api.${var.project}.keigo.io"
  depends_on = ["aws_s3_bucket.uberjar_bucket"]
  key = "${var.project}.jar"
  source = "target/${var.project}.jar"
  etag = "${filemd5("target/${var.project}.jar")}"
}

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
        "logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"
      ],
      "Effect": "Allow",
      "Resource": "arn:aws:logs:${var.AWS_REGION}:*:*"
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
  depends_on = ["aws_s3_bucket.uberjar_bucket",
                "aws_iam_role.lambda_role",
                "aws_s3_bucket_object.uberjar_file",
                "aws_iam_policy.lambda_policy"]
  s3_bucket          = "${aws_s3_bucket.uberjar_bucket.bucket}"
  s3_key             = "${var.project}.jar"
  function_name      = "${var.project}"
  description        = "API for ${var.project}.keigo.io"
  role               = "${aws_iam_role.lambda_role.arn}"
  handler            = "${var.project}.core.LambdaFunction"
  source_code_hash   = "${base64sha256("target/${var.project}.jar")}"
  runtime            = "java8"
  timeout            = 100
  memory_size        = 256
}