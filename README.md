# Canary

#### A repository for messing around with AWS Lambda and Terraform.

## Local development setup
- Set up the required environment variables:
  - `AUTHORISATION_CLIENT_ID`
  - `AUTHORISATION_CLIENT_SECRET`
  - `COOKIE_STORE_KEY`
  - `COOKIE_ATTRIBUTE_DOMAIN`
  - `COOKIE_ATTRIBUTE_SECURE`
  - `CORS_ORIGIN`
- Kick off a repl from the command line: `clj -A:nrepl`.
- Connect from within emacs: `C-c M-c`.
- Start the local server by evaluating `dev/canary/server.clj`.
- The following endpoints are now available:
  - `api.localhost/query`
  - `api.localhost/command`

## Deploy infrastructure
- First time around: `terraform init infrastructure`.
- To deploy changes: `terraform apply infrastructure`.
- To remove everything: `terraform destroy infrastructure`.

## Build and deploy an artifact
- Setup the required environment variables: 
  - `TF_VAR_authorisation_client_secret`
  - `TF_VAR_cookie_store_key`
- Clean averything up: `lein clean`.
- Build the artifact: `lein uberjar`.
- Deploy via terraform: `terraform apply infrastructure`.
