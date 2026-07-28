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

## Production on the Mini PC with Supabase

Production uses a separate API container and Docker volume from staging. It
does **not** start a local PostgreSQL container: the API connects to Supabase's
session pooler instead.

1. In the repository checkout used by the Mini PC runner, create the private
   environment file from the example:

   ```sh
   cp .env.production.example .env.production
   chmod 600 .env.production
   ```

2. Fill in the real values. Copy the Supabase **Session pooler / JDBC** details
   into `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Keep `DB_POOL_MIN_IDLE=0`
   and `DB_POOL_KEEPALIVE_MS=0`. Use the current production `JWT_SECRET` so
   existing app login tokens remain valid.

   If the current Render service has `FCM_ENABLED=true`, copy its Firebase JSON
   key to a private file on the Mini PC (outside the repository), then set:

   ```dotenv
   FCM_ENABLED=true
   USE_FIREBASE_SECRET_FILE=true
   FIREBASE_SERVICE_ACCOUNT_HOST_PATH=/home/<mini-pc-user>/bookmate-secrets/firebase-service-account.json
   FIREBASE_SERVICE_ACCOUNT_PATH=/run/secrets/firebase-service-account.json
   ```

   The deployment mounts that file read-only. Do not put the Firebase JSON in
   the repository or send it in chat. If `FCM_ENABLED=false`, leave all four
   Firebase file settings disabled/empty.

3. Before making the public DNS change, deploy and test the container locally:

   ```sh
   ./scripts/deploy-production.sh
   curl http://127.0.0.1:18081/health
   ```

   The shared Traefik container automatically adds HTTPS routing for
   `PRODUCTION_DOMAIN=api.bookmate.kr` when its Docker network exists. Do not
   change the Cloudflare record until this health check succeeds.

   To use the same local-build approach as the existing staging container,
   clone the repository on the Mini PC and use this command instead. It builds
   the Dockerfile on the Mini PC and does not require any GitHub Actions secret:

   ```sh
   BUILD_LOCAL=true API_IMAGE=bookmate-api:production ./scripts/deploy-production.sh
   ```

4. Optional but recommended: create a GitHub environment named `production`
   and add a `PRODUCTION_ENV_FILE` environment secret containing the full,
   private `.env.production` file. Then run the manual **Production deploy**
   workflow. It deliberately never deploys production on every push to `main`.

5. On cutover day, briefly prevent writes to the old Render API, run the final
   Neon-to-Supabase migration verification, point Cloudflare `api.bookmate.kr`
   at the Mini PC, and check:

   ```sh
   curl https://api.bookmate.kr/health
   ```

Keep Render and the Neon backup until the production API has been stable for a
few days. If `R2_ENABLED=false`, new profile images are stored in the named
`bookmate-production_profile_images` Docker volume. Existing profile images
hosted only on Render must be copied before cancelling Render.
