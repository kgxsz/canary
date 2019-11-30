# Canary

#### A repository for messing around with AWS Lambda and Terraform.

## Local development setup
- Kick off a repl from within emacs: `C-c M-j`:
  - Then specify `clojure-cli`.
  - Evaluate the `canary.server` namespace to serve the API.

## Deploy infrastructure
- First time around: `terraform init infrastructure`.
- To deploy changes: `terraform apply infrastructure`.
- To remove everything: `terraform destroy infrastructure`.

## Build and deploy an artifact
- Clean averything up: `lein clean`.
- Build the artifact: `lein uberjar`.
- Deploy via terraform: `terraform apply infrastructure`.
