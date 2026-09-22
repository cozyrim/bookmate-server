# 개발 환경 설정

## 테스트 실행

Java 21 설치 후 실행. 테스트는 H2 인메모리 DB를 사용하므로 PostgreSQL 설치나 운영 자격 증명 없이 실행 가능.

```sh
./gradlew --no-daemon test
```

## 로컬 서버 실행

1. `.env.staging.example`을 `.env.staging`으로 복사.
2. `POSTGRES_PASSWORD`, `JWT_SECRET`, 필요 시 `MODERATION_ADMIN_SECRET`을 본인 개발용 값으로 설정. JWT 키는 최소 32바이트의 무작위 값 사용.
3. `PUBLIC_BASE_URL=http://127.0.0.1:18080`, `API_BIND_ADDRESS=127.0.0.1`로 설정.
4. 카카오 로그인을 검증하려면 Kakao Developers에서 **숫자 앱 ID**를 확인해 `KAKAO_APP_ID`에 입력. Native App Key·REST API 키와는 다른 값이며, 비어 있으면 카카오 로그인 요청은 거부.
5. FCM·Discord·R2는 사용하지 않으면 비활성화하거나 값을 비워 둔 상태로 실행.

```sh
cp .env.staging.example .env.staging
chmod 600 .env.staging
# 아래 명령으로 무작위 키를 생성해 .env.staging에 입력
openssl rand -base64 48

docker compose -f docker-compose.staging.yml -f docker-compose.staging.local.yml --env-file .env.staging up -d --build
curl http://127.0.0.1:18080/health
```

정상 상태의 `/health` 응답은 `OK`. `.env.staging`은 Git 추적에서 제외되며 Spring이 파일을 직접 읽는 것이 아니라 Docker Compose가 환경변수로 전달.

## 외부 서비스 설정

| 설정 | 용도 |
| --- | --- |
| `JWT_SECRET` | 북메이트 access token 서명. 누락·짧은 키·예시 값은 시작 시 거부 |
| `KAKAO_APP_ID` | 카카오 토큰이 북메이트 앱에서 발급됐는지 확인 |
| `APPLE_CLIENT_ID` | Apple identity token의 audience와 비교할 iOS Bundle Identifier |
| `FCM_ENABLED` | FCM 사용 여부. 사용 시 서비스 계정 설정 필요 |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | 저장소 밖에 보관한 서버용 서비스 계정 파일 경로 |
| `R2_*` | 이미지 저장 버킷과 해당 버킷 전용 자격 증명 |
| `MODERATION_ADMIN_SECRET` | 관리자 API 전용 키. 비어 있으면 관리자 API 비활성화 |
| `SWAGGER_ENABLED` | 개발용 API 문서. 기본 `false` |

서버의 Firebase 서비스 계정 JSON은 iOS의 `GoogleService-Info.plist`와 다름. **서비스 계정 JSON에는 비밀키가 포함되므로 공개 금지.**

운영용 예시는 `.env.production.example` 참고. 실제 운영 서명키를 바꾸면 기존 로그인 토큰이 무효화되므로, 공개 준비를 이유로 운영 키를 임의 변경하지 않음.

[README](../README.md)
