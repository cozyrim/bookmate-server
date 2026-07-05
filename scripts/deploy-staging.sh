#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.staging}"
API_IMAGE="${API_IMAGE:-ghcr.io/cozyrim/bookmate-server:staging}"
USE_TRAEFIK="${USE_TRAEFIK:-auto}"
IMAGE_PRUNE_UNTIL="${IMAGE_PRUNE_UNTIL:-168h}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing staging environment file: $ENV_FILE" >&2
  echo "Create it from .env.staging.example or provide STAGING_ENV_FILE in GitHub Actions secrets." >&2
  exit 1
fi

compose=(docker compose -f docker-compose.staging.yml)

if [[ "$USE_TRAEFIK" == "true" ]] || {
  [[ "$USE_TRAEFIK" == "auto" ]] && docker network inspect traefik_public_network >/dev/null 2>&1
}; then
  compose+=(-f docker-compose.staging.traefik.yml)
fi

echo "Deploying API image: $API_IMAGE"

API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" pull api
API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" up -d --remove-orphans

container_id="$(API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" ps -q api)"
if [[ -z "$container_id" ]]; then
  echo "API container was not created." >&2
  exit 1
fi

for _ in {1..30}; do
  health_status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id")"

  case "$health_status" in
    healthy|running)
      API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" ps
      docker image prune -f --filter "until=$IMAGE_PRUNE_UNTIL"
      exit 0
      ;;
    unhealthy|exited|dead)
      echo "API container became $health_status." >&2
      API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" logs --tail=120 api >&2
      exit 1
      ;;
  esac

  sleep 5
done

echo "Timed out waiting for API container health check." >&2
API_IMAGE="$API_IMAGE" "${compose[@]}" --env-file "$ENV_FILE" logs --tail=120 api >&2
exit 1
