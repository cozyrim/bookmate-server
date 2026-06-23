# bookMate-mini-server

## Staging with Docker Compose

1. Create the staging env file:

```sh
cp .env.staging.example .env.staging
```

2. Edit `.env.staging` and replace every `change-this...` value.

You can generate secrets with:

```sh
openssl rand -base64 48
```

3. Build and run the staging stack:

```sh
docker compose -f docker-compose.staging.yml --env-file .env.staging up -d --build
```

4. Check the API locally:

```sh
curl http://127.0.0.1:18080/health
```

The expected response is:

```txt
OK
```

If `staging-api.bookmate.kr` points to this server and ports 80/443 are open,
Caddy will proxy HTTPS traffic to the API:

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
