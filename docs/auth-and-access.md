# 인증·접근 권한·운영 설정 기준

## 두 단계의 권한 확인

**인증은 누구인지 확인하는 단계, 접근 권한은 그 사람이 해당 기록을 볼 수 있는지 확인하는 단계.** 정상 토큰이 있어도 다른 사용자의 비공개 책이나 메모를 조회·수정할 수 있으면 안 됨.

| 대상 | 코드의 확인 방식 |
| --- | --- |
| 개인 책·단어·문장·리뷰 | 서버에서 확인한 사용자 ID를 조회 조건에 포함 |
| 독서 메모 | 생성 시 책 소유권 확인, 수정·삭제 시 메모 소유권 확인 |
| 공개 사용자·책장 | 공개 여부·탈퇴 여부·차단·신고 조건 확인 |
| 방명록 삭제 | 글 작성자 또는 방 주인인지 확인 |
| 알림함 | 로그인한 사용자 ID로 조회·읽음 처리 |
| 관리자 API | 별도 관리자 키 확인. 미설정 시 비활성화 |

`AuthInterceptor`는 JWT 서명·만료와 사용자 존재·탈퇴 여부를 확인하고, Controller는 `CurrentUserResolver`에서 확인된 사용자를 전달받음. 새로운 API를 추가할 때도 인증과 소유권 검사가 빠지지 않도록 검증 필요. 장기적으로 인증 경로 관리는 Servlet filter 기반 Spring Security로 통합하는 선택지도 있음.

## 소셜 로그인과 업로드

- Apple: 서명·발급자·audience·만료·사용자 식별자를 검증하고 이메일은 검증된 토큰의 claim만 사용.
- Kakao: 토큰 정보의 앱 ID·남은 수명 확인 후 사용자 조회 결과의 ID까지 대조.
- 프로필 이미지: JPEG·PNG만 허용하고 실제 바이트의 형식과 이미지 크기 검사. 파일 2MB, 이미지 2천만 픽셀 제한. 저장 경로는 서버가 UUID로 생성.
- 관리자 키와 JWT 키, Firebase 서비스 계정, R2 자격 증명은 클라이언트에 전달하지 않음.

## 운영에서 별도로 적용할 기준

아래는 권장 설정이며 이 문서를 추가한다고 운영 서버에 자동 적용되는 것은 아님.

| 항목 | 설정 기준 |
| --- | --- |
| 외부 연결 | 클라이언트는 HTTPS만 사용. DB 포트는 인터넷에 직접 공개하지 않음 |
| DB 계정 | 애플리케이션에 필요한 권한만 부여. Supabase 관리 API 키와 JDBC 비밀번호는 서버에만 보관 |
| Supabase Data API | 사용하지 않으면 비활성화 또는 노출 스키마 제한. 사용하는 테이블에는 RLS·권한 점검. 서버의 JDBC 접근 권한과는 별개 |
| 서명·관리자 키 | 환경변수 또는 비밀 저장소에 보관. 예시 값 사용 금지. 관리자 API는 가능하면 VPN·IP 제한 추가 |
| API 문서 | 운영은 Swagger 비활성화 |
| 이미지 저장소 | 자격 증명은 사용하는 버킷 범위로 제한. 공개 읽기와 파일 쓰기 권한 구분 |
| Firebase | 서버 서비스 계정은 필요한 FCM 권한만 사용. 클라이언트 API 키는 필요한 Firebase API만 허용 |
| 로그 | 비밀번호·Authorization·refresh token·서비스 계정 JSON 기록 금지 |
| 공개 CI | GitHub 제공 runner에서 테스트만 실행. 운영 장비·비밀값을 공개 PR 실행 환경에 연결하지 않음 |

Firebase Admin SDK의 권한은 서버 서비스 계정 IAM으로 관리. Firestore Security Rules가 서버 Admin SDK의 모든 접근을 제한해주는 구조가 아님. 이 서버의 사용자 기록은 JPA/PostgreSQL 경로이므로 서버의 소유권 검사와 DB 권한이 직접적인 경계.

## 요청 횟수 제한

2026-09-25 운영 Traefik에 로그인·회원가입 제한 적용. 애플리케이션 코드를 추가하지 않고 북메이트 라우터와 미들웨어만 설정.

| 요청 | IP별 충전 속도 | 한 번에 허용하는 최대 요청 수 |
| --- | --- | --- |
| 이메일·카카오·Apple 로그인 합산 | 분당 30회 | 20회 |
| 회원가입 | 10분당 10회 | 5회 |

토큰 버킷 방식으로 여유분을 소진하면 `429`와 `Retry-After` 반환. 고정된 1분 구간의 총 요청 수를 30회로 제한하는 방식은 아님. IPv6는 `/64` 단위로 묶어 주소 변경을 통한 우회 범위 축소.

- **판단**: 로그인 전 호출에도 제한이 필요하지만, 이미 출시한 앱의 정상 세션을 끊지 않는 것이 우선.
- **해결 과정**: 직접 연결된 클라이언트 IP를 기준으로 제한. 임의의 `X-Forwarded-For`·`X-Real-IP`·`CF-Connecting-IP`는 기준으로 사용하지 않음. 경로 뒤 슬래시·세미콜론 변형도 로그인 제한에 포함.
- **결과 및 배운 점**: 격리 프록시 검증 20개, 운영 검증 9개 통과. 횟수 제한뿐 아니라 우회 가능성과 기존 앱의 오류 응답 해석까지 함께 확인할 필요.

출시된 iOS 앱은 토큰 갱신의 모든 `4xx`를 인증 실패로 간주하므로 **refresh는 이번 제한에서 제외**. 클라이언트가 `429`를 재시도 가능한 오류로 구분하도록 수정·배포한 뒤 별도 제한 검토. 일반 책·메모 요청과 상태 확인 경로도 이번 제한 대상에서 제외.

현재 클라이언트가 Traefik에 직접 연결되는 경로를 기준으로 설정. CDN이나 프록시를 추가하면 실제 사용자 IP 전달 방식과 신뢰 범위 재검토 필요. 이동통신·공용 Wi-Fi의 정상 사용자 차단율을 보고 수치 조정. 계정별 로그인 실패 제한과 사용자별 업로드·작성량 제한은 별도 개선 대상.

CORS는 브라우저 정책이므로 iOS 앱이나 직접 API 호출의 접근 제어를 대신하지 못함.

## 운영 적용과 검증 — 2026-09-22

- 북메이트 테이블 11개의 RLS 활성화 및 `anon`·`authenticated`·`PUBLIC` 직접 접근 권한 회수. 서버의 JDBC 접근은 유지하고 실제 DB 조회 응답 확인.
- [DB 접근 제한 SQL](../scripts/sql/20260922-server-only-db-access.sql)은 서버를 통해서만 테이블에 접근하는 구조에 맞춘 설정. 다른 프로젝트에 적용하려면 클라이언트의 Data API 사용 여부와 서버 계정 권한부터 확인.
- 기존 테이블의 설정만 변경. 새 테이블을 추가할 때도 RLS·권한을 별도 검증해야 하며, DB 전체의 기본 권한은 변경하지 않음.
- PostgreSQL 17 격리 환경에서 로그인·소유권·업로드·토큰 재발급 등 19개 항목 검증. 운영에서는 HTTPS 응답, 인증 없는 요청 거부, 잘못된 소셜 로그인 토큰 거부 확인.
- 공유 서버에서 북메이트 운영 API만 교체. 기존 자격 증명·업로드 볼륨 유지, 다른 컨테이너의 재시작 없음 확인.
- 배포 후 사용자가 실기기 카카오 로그인과 책·메모 저장 정상 동작 확인. Apple 로그인과 실제 푸시 수신은 추가 확인 대상.

## 운영 권한 점검 — 2026-09-25

| 대상 | 적용·확인 내용 | 검증 범위 |
| --- | --- | --- |
| Firebase 서버 계정 | 기존 Admin SDK 관리자·서비스 계정 토큰 생성자 역할을 FCM API 관리자 역할로 축소 | 운영 자격 증명으로 FCM `validate_only` 성공. 점검한 데이터 삭제·사용자 수정·계정 토큰 생성 권한 없음 확인 |
| Firebase iOS API 키 | 북메이트 Bundle Identifier로 앱 제한 추가, 기존 API 허용 목록 유지 | 실제 앱 구성의 키로 올바른 식별자 200·다른 식별자 403 확인. 검증용 설치 등록은 즉시 삭제 |
| R2 | 운영 프로필 이미지 버킷 하나의 객체 읽기·쓰기 권한 유지 | 콘솔 정책과 운영 Access Key ID 대조. 키 교체·권한 확대 없음 |
| 운영 컨테이너 | 북메이트 API의 Traefik 설정만 변경 | 기존 이미지·환경변수·볼륨 유지. 공유 프록시와 다른 컨테이너 8개의 재시작 없음 확인 |

iOS API 키 제한은 앱 식별자에 대한 사용 범위 설정이며, 위조 불가능한 앱 인증을 제공하는 것은 아님. 서버 인증·소유권 검사와 IAM을 대체하지 않음.

FCM 검증은 실제 알림을 보내지 않는 모드로 수행. 실제 APNs 전달과 기기 수신까지 확인한 결과는 아님. 최소 권한 설정은 키 유출 시 피해 범위를 줄이는 조치이며, 서비스 계정 키를 저장소에 공개해도 된다는 의미는 아님.

관련 자료: [Traefik 요청 제한](https://doc.traefik.io/traefik/reference/routing-configuration/http/middlewares/ratelimit/), [FCM 검증 모드](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages/send), [Supabase Data API 보안](https://supabase.com/docs/guides/api/securing-your-api), [Firebase 서버 접근과 IAM](https://firebase.google.com/docs/firestore/security/rules-conditions), [Firebase API 키](https://firebase.google.com/docs/projects/api-keys), [OWASP 인증](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), [OWASP 파일 업로드](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html), [Kakao REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api), [Spring HandlerInterceptor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/HandlerInterceptor.html).

[README](../README.md)
