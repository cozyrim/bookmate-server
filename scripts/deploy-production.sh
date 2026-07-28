#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.production}"
API_IMAGE="${API_IMAGE:-}"
USE_TRAEFIK="${USE_TRAEFIK:-auto}"
USE_FIREBASE_SECRET_FILE="${USE_FIREBASE_SECRET_FILE:-}"
BUILD_LOCAL="${BUILD_LOCAL:-false}"
IMAGE_PRUNE_UNTIL="${IMAGE_PRUNE_UNTIL:-168h}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing production environment file: $ENV_FILE" >&2
  echo "Create it from .env.production.example or provide PRODUCTION_ENV_FILE in GitHub Actions secrets." >&2
  exit 1
fi

compose=(docker compose -f docker-compose.production.yml)

if [[ "$BUILD_LOCAL" == "true" ]]; then
  compose+=(-f docker-compose.production.local.yml)
  API_IMAGE="${API_IMAGE:-bookmate-api:production}"
else
  API_IMAGE="${API_IMAGE:-ghcr.io/cozyrim/bookmate-server:production}"
fi

if [[ -z "$USE_FIREBASE_SECRET_FILE" ]]; then
  USE_FIREBASE_SECRET_FILE="$(sed -nE 's/^USE_FIREBASE_SECRET_FILE=(true|false)$/\1/p' "$ENV_FILE" | tail -n 1)"
fi
USE_FIREBASE_SECRET_FILE="${USE_FIREBASE_SECRET_FILE:-false}"

if [[ "$USE_FIREBASE_SECRET_FILE" == "true" ]]; then
  compose+=(-f docker-compose.production.firebase.yml)
fi

if [[ "$USE_TRAEFIK" == "true" ]] || {
  [[ "$USE_TRAEFIK" == "auto" ]] && docker network inspect traefik_public_network >/dev/null 2>&1
}; then
  compose+=(-f docker-compose.production.traefik.yml)
fi

echo "Deploying API image: $API_IMAGE"

if [[ "$BUILD_LOCAL" == "true" ]]; then
  BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" up -d --build --remove-orphans
else
  BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" pull api
  BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" up -d --remove-orphans
fi

container_id="$(BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" ps -q api)"
if [[ -z "$container_id" ]]; then
  echo "API container was not created." >&2
  exit 1
fi

for _ in {1..30}; do
  health_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")"

  case "$health_status" in
    healthy|running)
      BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" ps
      docker image prune -f --filter "until=$IMAGE_PRUNE_UNTIL"
      exit 0
      ;;
    unhealthy|exited|dead)
      echo "API container became $health_status." >&2
      BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" logs --tail=120 api >&2
      exit 1
      ;;
  esac

  sleep 5
done

echo "Timed out waiting for API container health check." >&2
BOOKMATE_ENV_FILE="$ENV_FILE" API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" logs --tail=120 api >&2
exit 1
