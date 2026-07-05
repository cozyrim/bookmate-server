# bookMate-mini-server

## Staging with Docker Compose

1. Create the staging env file:

```sh
cp .env.staging.example .env.staging
```

2. Edit `.env.staging` and replace every `change-this...` value.
   Set `DISCORD_LOGIN_WEBHOOK_URL` only if login notifications should be sent
   to a private Discord channel.

You can generate secrets with:

```sh
openssl rand -base64 48
```

3. Pull and run the staging stack:

```sh
docker compose -f docker-compose.staging.yml --env-file .env.staging pull api
docker compose -f docker-compose.staging.yml --env-file .env.staging up -d
```

On a server that already has a shared Traefik ingress network, use the Traefik
override instead:

```sh
docker compose \
  -f docker-compose.staging.yml \
  -f docker-compose.staging.traefik.yml \
  --env-file .env.staging \
  pull api

docker compose \
  -f docker-compose.staging.yml \
  -f docker-compose.staging.traefik.yml \
  --env-file .env.staging \
  up -d
```

For local staging builds from the current checkout, add the local build override:

```sh
docker compose \
  -f docker-compose.staging.yml \
  -f docker-compose.staging.local.yml \
  --env-file .env.staging \
  up -d --build
```

4. Check the API locally:

```sh
curl http://127.0.0.1:18080/health
```

The expected response is:

```txt
OK
```

If `staging-api.bookmate.kr` points to this server and the shared Traefik
container owns ports 80/443, Traefik will proxy HTTPS traffic to the API:

```sh
curl https://staging-api.bookmate.kr/health
```

5. View logs:

```sh
docker compose -f docker-compose.staging.yml --env-file .env.staging logs -f api
```

6. Stop the stack:

```sh
docker compose -f docker-compose.staging.yml --env-file .env.staging down
```

Do not commit `.env.staging`. Commit only `.env.staging.example`.

## Staging CI/CD

GitHub Actions runs staging delivery from `.github/workflows/staging.yml`.

- Pull requests to `main` run `./gradlew --no-daemon clean test`.
- Pushes to `main` and manual workflow runs test first, then build and push a Docker image to GitHub Container Registry.
- Images are tagged as `ghcr.io/cozyrim/bookmate-server:staging` and `ghcr.io/cozyrim/bookmate-server:<commit-sha>`.
- Deployment uses the immutable commit SHA image, then waits for the `/health` Docker health check.

Mini PC setup:

1. Install Docker and Docker Compose.
2. Install a GitHub Actions self-hosted runner for this repository on the mini PC.
3. Add the runner label `bookmate-staging`.
4. Make sure the runner user can run Docker commands.
5. Create a GitHub environment named `staging`.
6. Add an environment secret named `STAGING_ENV_FILE` whose value is the full contents of `.env.staging`.

The deployment script is `scripts/deploy-staging.sh`. It automatically uses
`docker-compose.staging.traefik.yml` when the `traefik_public_network` Docker
network exists. Set `USE_TRAEFIK=true` or `USE_TRAEFIK=false` in the workflow if
you want to force either behavior.
