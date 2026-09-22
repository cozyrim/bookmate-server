# 방명록 저장과 알림 발송 분리

## 문제와 판단

방명록 저장 요청 안에서 외부 FCM 발송까지 기다리면, 알림 서비스 지연이 글 저장 응답에 영향을 줌. 저장이 최종 확정되기 전에 알림을 발송하면 취소된 글의 알림이 전달될 가능성도 있음.

저장 성공을 먼저 확정하고 그 이후 알림을 요청하는 방식 선택.

## 해결 과정

```mermaid
sequenceDiagram
    participant App as iOS
    participant Service as GuestbookService
    participant DB as PostgreSQL
    participant Push as PushNotificationService
    App->>Service: 방명록 작성
    Service->>DB: 글 저장
    DB-->>Service: 트랜잭션 커밋
    Service->>Push: afterCommit에서 비동기 발송 요청
    Service-->>App: 저장된 글 응답
    Push->>Push: 알림함 기록과 FCM 발송 처리
```

- `TransactionSynchronization.afterCommit()`에서 발송 요청.
- 다른 Bean의 `@Async` 메서드를 호출해 Spring 비동기 프록시 적용.
- 엔티티 전체 대신 사용자·메시지 ID와 필요한 작성자 정보를 전달.
- iOS는 임시 글을 먼저 표시하고 서버 응답으로 교체해 화면 반응도 별도로 처리.

## 결과 및 배운 점

개인 노션 「방명록 전송버튼 개선」의 당시 실기기 측정 기록 확인. iOS 낙관적 갱신과 서버 FCM 비동기화를 함께 적용한 전후 각각 22회 결과에서 API 구간 p50은 735ms → 141.20ms(80.8% 감소), p95는 1,280ms → 235.52ms(81.6% 감소).

클라이언트 `os_signpost` 구간이므로 서버 단독 실행 시간이나 FCM 비동기화만의 기여율은 아님. [iOS 문서의 전체 표와 측정 범위](https://github.com/cozyrim/bookmate-ios-public/blob/main/docs/guestbook-send-performance.md) 참고.

저장 응답에서 외부 작업을 분리할 때는 실행 시점도 함께 정할 필요. `afterCommit`은 저장 확정 이후의 발송 순서를 정하지만 전달 자체를 보장하지는 않음.

현재는 애플리케이션 내부 비동기 실행. 프로세스 종료 시 전달 보장, 영속 재시도, FCM 중복 발송 방지를 모두 해결하는 구조는 아님. 작업 큐나 outbox 도입은 알림 전달 보장이 필요한 수준에 따라 판단할 후속 과제.

관련 코드: [GuestbookService](../src/main/java/com/exercise/bookmateserver/social/guestbook/GuestbookService.java), [PushNotificationService](../src/main/java/com/exercise/bookmateserver/notification/PushNotificationService.java).

[README](../README.md)
