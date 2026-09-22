# Refresh token 교체와 동시 요청

## 판단 기준

access token 만료마다 사용자가 다시 로그인해야 하는 흐름을 줄이면서, 재발급 자격 증명이 DB에 원문으로 저장되지 않도록 설계.

- access token: HMAC-SHA256 서명과 만료 시각 검사.
- refresh token: 32바이트 난수 생성 후 URL-safe Base64 인코딩. DB에는 SHA-256 해시와 만료 시각만 저장.
- 사용자마다 현재 refresh token 하나를 보관. 별도 세션 테이블 없이 현재 구조에서 관리하는 선택.

## 갱신 순서

1. 전달받은 refresh token의 해시 계산.
2. 해당 해시와 일치하는 미탈퇴 사용자 조회. `PESSIMISTIC_WRITE`로 행 잠금.
3. 저장된 만료 시각 확인.
4. 새 refresh token의 해시·만료 시각으로 교체하고 새 access token과 함께 반환.

이전 토큰의 해시는 교체되므로 이후 같은 값으로 다시 조회하면 인증 실패. 잠금과 변경은 같은 트랜잭션에서 처리. iOS에서도 동시 401 요청이 하나의 갱신 작업을 공유하도록 구성.

## 검증과 한계

- 단위 테스트에서 정상 토큰 교체와 미등록 토큰 거부 확인.
- access token 변조·만료·잘못된 서명키 거부 테스트 포함.
- PostgreSQL 17 격리 환경에서 같은 refresh token으로 동시 요청 2개 전송. 한 요청만 성공(`200`), 다른 요청은 거부(`401`)됨을 확인. 모든 부하·장애 상황을 검증한 결과는 아님.
- 사용자별 토큰 한 개이므로 다른 기기에서 로그인하면 이전 기기의 refresh token 대체.
- 현재 로그아웃 API는 서버 토큰 폐기를 수행하지 않음. access token은 만료 전까지, refresh token은 교체·만료 전까지 유효할 수 있으므로 서버 로그아웃 폐기는 후속 보완 대상.
- JWT 발급 구조와 기존 만료 시간은 유지. 수명 단축이나 세션 테이블 도입은 운영 정책과 함께 검토할 항목.

## 기존 운영 DB에 적용

- [컬럼 추가 SQL](../scripts/sql/20260922-refresh-columns.sql): nullable 컬럼 2개와 고유 인덱스 추가. 기존 사용자 데이터 수정 없음.
- 운영 DB의 구조만 복제한 PostgreSQL 17에서 SQL 재실행과 새 서버의 스키마 검증 확인. 사용자 데이터는 복제하지 않음.
- 운영은 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`로 실행. 스키마 변경은 별도 SQL로 적용.
- API를 이전 이미지로 복구해도 추가한 컬럼은 유지. 기존 버전은 해당 컬럼을 사용하지 않음.

관련 코드: [TokenService](../src/main/java/com/exercise/bookmateserver/auth/TokenService.java), [AuthService](../src/main/java/com/exercise/bookmateserver/auth/AuthService.java), [UserRepository](../src/main/java/com/exercise/bookmateserver/user/UserRepository.java).

[README](../README.md)
