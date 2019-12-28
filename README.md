# Canary

#### A repository for messing around with AWS Lambda and Terraform.

## Local development setup
- Set up the required environment variables:
  - AUTHOIRSATION_CLIENT_ID
  - AUTHOIRSATION_CLIENT_SECRET
  - COOKIE_STORE_KEY
- Kick off a repl from the command line: `clj -A:nrepl`.
- Then connect from within emacs: `C-c M-c` 

## Deploy infrastructure
- First time around: `terraform init infrastructure`.
- To deploy changes: `terraform apply infrastructure`.
- To remove everything: `terraform destroy infrastructure`.

## Build and deploy an artifact
- Setup the required environment variables: 
  - TF_VAR_authorisation_client_secret
  - TF_VAR_cookie_store_key
- Clean averything up: `lein clean`.
- Build the artifact: `lein uberjar`.
- Deploy via terraform: `terraform apply infrastructure`.
