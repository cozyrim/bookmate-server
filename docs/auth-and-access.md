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

로그인·회원가입처럼 인증 전에도 호출되는 API는 무차별 대입과 자동 가입에 대비한 제한 필요. Cloudflare 또는 앞단 프록시에서 우선 적용하고, 여러 서버에 걸친 계정별 제한이 필요해질 때 별도 저장소 검토.

| 요청 | 초기 검토값의 예시 |
| --- | --- |
| 로그인 | IP당 분당 30회, 계정별 연속 실패는 지연·추가 확인 적용 |
| 회원가입 | IP당 10분에 10회 |
| refresh | 일반 읽기보다 낮은 별도 한도. 동시 화면 요청이 공유하는 갱신 흐름 고려 |
| 이미지·방명록 작성 | 사용자별 생성·저장량과 분당 요청 수 제한 |

숫자는 정답이 아니라 시작점. 이동통신·공용 Wi-Fi에서는 여러 사용자가 같은 IP를 사용할 수 있어 정상 사용자 차단율을 보고 조정. 제한 시 `429`와 `Retry-After` 안내. iOS API에 브라우저용 CAPTCHA HTML을 그대로 반환하면 앱이 처리하지 못할 수 있음.

`X-Forwarded-For` 등 클라이언트가 임의로 보낸 헤더를 그대로 IP 기준으로 신뢰하면 안 됨. 실제 프록시 경로와 신뢰할 프록시 설정을 먼저 확인. CORS는 브라우저 정책이므로 iOS 앱이나 직접 API 호출의 접근 제어를 대신하지 못함.

관련 자료: [Supabase Data API 보안](https://supabase.com/docs/guides/api/securing-your-api), [Firebase 서버 접근과 IAM](https://firebase.google.com/docs/firestore/security/rules-conditions), [Firebase API 키](https://firebase.google.com/docs/projects/api-keys), [OWASP 인증](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), [OWASP 파일 업로드](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html), [Kakao REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api), [Spring HandlerInterceptor](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/HandlerInterceptor.html).

[README](../README.md)
