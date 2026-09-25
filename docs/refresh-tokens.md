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
- 로그아웃 시 전달한 refresh token의 해시와 일치하는 자격 증명 폐기. 이미 폐기된 토큰도 `204` 반환. 지연된 이전 로그아웃이 새 로그인을 무효화하지 않도록 사용자 ID만으로 일괄 삭제하지 않음.
- 기존 앱과의 호환을 위해 본문 없는 로그아웃은 `204` 유지. 실제 서버 폐기는 refresh token을 보내는 새 iOS 버전에서 동작.
- access token은 기존 만료 전까지 유효. 이번 변경은 refresh token 폐기이며 모든 access token의 즉시 무효화를 구현한 것은 아님.
- 로그아웃과 갱신이 겹치면 iOS가 저장 전 현재 refresh token을 다시 확인. 폐기된 세션의 응답은 저장하지 않고 회전된 토큰도 폐기 요청. 연결 실패·앱 종료 시 서버 폐기를 보장하지는 못하지만 로컬 토큰은 즉시 삭제.
- 갱신 응답의 `401`만 인증 만료로 해석. `429`·일시적 서버 오류는 토큰을 보존. 계정 전환 전의 진행 중 요청도 새 세션을 변경하지 않도록 취소.
- JWT 발급 구조와 기존 만료 시간은 유지. 수명 단축이나 세션 테이블 도입은 운영 정책과 함께 검토할 항목.

## 기존 운영 DB에 적용

- [컬럼 추가 SQL](../scripts/sql/20260922-refresh-columns.sql): nullable 컬럼 2개와 고유 인덱스 추가. 기존 사용자 데이터 수정 없음.
- 운영 DB의 구조만 복제한 PostgreSQL 17에서 SQL 재실행과 새 서버의 스키마 검증 확인. 사용자 데이터는 복제하지 않음.
- 운영은 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`로 실행. 스키마 변경은 별도 SQL로 적용.
- API를 이전 이미지로 복구해도 추가한 컬럼은 유지. 기존 버전은 해당 컬럼을 사용하지 않음.

관련 코드: [TokenService](../src/main/java/com/exercise/bookmateserver/auth/TokenService.java), [AuthService](../src/main/java/com/exercise/bookmateserver/auth/AuthService.java), [UserRepository](../src/main/java/com/exercise/bookmateserver/user/UserRepository.java).

[README](../README.md)
