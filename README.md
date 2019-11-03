# Canary

#### A repository for messing around with AWS Lambda and Terraform.

## Deploy Infrastructure
- First time around: `terraform init infrastructure`.
- To deploy changes: `terraform apply infrastructure`.
- To remove everything: `terraform destroy infrastructure`.

## Build and deploy an artifact
- Clean averything up: `lein clean`.
- Build the artifact: `lein uberjar`.
- Deploy via terraform: `terraform apply infrastructure`.
