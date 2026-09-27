## 1. 기술 스택

- Java 25
- Spring Boot 4.0.1
- Spring Data JPA
- H2 Database
- Lombok
- Gradle

---

## 2. 실행 방법

### 실행

Git Bash / macOS / Linux

```bash
./gradlew bootRun

Windows CMD
gradlew.bat bootRun

DB 설정
개발 환경에서는 H2 파일 DB를 사용합니다.
spring:
  datasource:
    url: jdbc:h2:./db_dev;MODE=MySQL
    username: sa
    password:

애플리케이션은 기본적으로 8080 포트에서 실행됩니다.
3. 프로젝트 구조
회원과 게시글을 각각 bounded context로 분리했습니다.
com.back
├─ boundedContext
│  ├─ member
│  │  ├─ in
│  │  ├─ app
│  │  ├─ domain
│  │  └─ out
│  │
│  └─ post
│     ├─ in
│     ├─ app
│     ├─ domain
│     └─ out
│
├─ shared
│  ├─ member
│  └─ post
│
└─ global

각 영역의 역할은 다음과 같습니다.
in
→ Controller, EventListener, 초기화 코드 등 입력의 시작점

app
→ Facade, UseCase 등 애플리케이션 기능 실행

domain
→ Entity, Policy 등 핵심 비즈니스 규칙

out
→ Repository 등 외부 저장소 접근

4. Facade와 UseCase
외부 입력은 각 모듈의 Facade를 통해 기능을 호출합니다.
회원 가입 흐름
MemberDataInit
    ↓
MemberFacade
    ↓
MemberJoinUseCase
    ↓
MemberRepository

글 작성 흐름
PostDataInit
    ↓
PostFacade
    ↓
PostWriteUseCase
    ↓
PostRepository

Facade는 모듈의 진입점과 트랜잭션 경계를 담당하고,
UseCase는 실제 업무 기능을 담당합니다.
5. 초기 데이터
최초 실행 시 다음 회원 6명을 생성합니다.
system
holding
admin
user1
user2
user3

글은 총 6개를 생성합니다.
user1 → 3개
user2 → 2개
user3 → 1개

댓글은 총 8개를 생성합니다.
초기 데이터는 Repository에 직접 저장하지 않고 실제 회원 가입, 글 작성, 댓글 작성 기능을 호출해서 만듭니다.
같은 DB로 다시 실행하는 경우 회원과 글이 이미 존재하는지 확인하여 데이터가 중복 생성되지 않도록 했습니다.
초기화 순서는 다음과 같습니다.
@Order(1)
MemberDataInit
    ↓
회원 6명 가입
    ↓
MemberJoinedEvent
    ↓
PostMember 생성

@Order(2)
PostDataInit
    ↓
PostMember 조회
    ↓
글과 댓글 생성

6. 댓글 생성
댓글은 별도의 댓글 Repository 저장 호출을 사용하지 않고 Post의 도메인 메서드로 생성합니다.
post.addComment(author, content);

Post와 PostComment의 연관관계에 cascade를 적용하여 글의 생명주기 안에서 댓글을 저장하고 삭제하도록 구성했습니다.
7. RsData와 DomainException
회원 가입과 글 작성의 성공 결과는 RsData<T>로 반환합니다.
resultCode
msg
data

중복 username으로 가입을 시도하면 다음 결과코드를 가진 DomainException을 발생시킵니다.
409-1

DomainException은 RuntimeException을 상속합니다.
8. 이벤트를 이용한 활동점수 처리
게시글 작성 시 작성자에게 3점, 댓글 작성 시 작성자에게 1점을 추가합니다.
Post 작성
    ↓
PostCreatedEvent
    ↓
MemberEventListener
    ↓
Member 활동점수 +3

댓글은 다음 흐름으로 처리됩니다.
Post.addComment()
    ↓
PostCommentCreatedEvent
    ↓
MemberEventListener
    ↓
Member 활동점수 +1

모듈 간 이벤트에는 엔티티를 직접 전달하지 않고 DTO를 담아서 전달합니다.
이벤트 리스너에는 다음 설정을 사용합니다.
@TransactionalEventListener(phase = AFTER_COMMIT)
@Transactional(propagation = REQUIRES_NEW)

따라서 글 또는 댓글 작성 트랜잭션이 정상적으로 커밋된 이후 별도의 트랜잭션에서 회원 활동점수를 변경합니다.
작성 트랜잭션 자체가 롤백되면 해당 이벤트 처리가 실행되지 않아 활동점수도 증가하지 않습니다.
9. Event와 HTTP API를 구분한 이유
모듈 간 통신은 목적에 따라 두 가지 방식을 사용했습니다.
Event
반환값이 필요하지 않고 다른 모듈에 사건 발생 사실만 알려주면 되는 경우 사용합니다.
글 작성됨
→ PostCreatedEvent

댓글 작성됨
→ PostCommentCreatedEvent

회원 가입됨
→ MemberJoinedEvent

회원 정보 변경됨
→ MemberModifiedEvent

HTTP API
호출한 모듈이 즉시 값을 받아 다음 로직에서 사용해야 하는 경우 HTTP API를 사용합니다.
글 작성 결과에 회원 모듈의 보안 팁을 포함해야 하기 때문에 post 모듈은 MemberApiClient를 이용해 회원 모듈의 API를 실제 HTTP로 호출합니다.
PostWriteUseCase
    ↓
MemberApiClient
    ↓ HTTP
ApiV1MemberController
    ↓
MemberFacade
    ↓
보안 팁 반환

API 주소
GET /api/v1/member/members/randomSecureTip

10. MemberPolicy
비밀번호 변경 주기 90일은 코드 여러 곳에 직접 작성하지 않고 MemberPolicy에서 관리합니다.
private static final int PASSWORD_CHANGE_DAYS = 90;

보안 팁 역시 이 정책값을 이용해 생성합니다.
예시
비밀번호의 유효기간은 90일 입니다.

11. PostMember 회원 복제
post 모듈이 member 모듈의 Member 엔티티를 직접 참조하지 않도록 회원 복제본 PostMember를 사용합니다.
member 모듈
Member

        ↓ Event + DTO

post 모듈
PostMember

회원 가입 시
MemberJoinUseCase
    ↓
MemberJoinedEvent
    ↓
PostEventListener
    ↓
PostFacade.syncMember()
    ↓
POST_MEMBER 저장

다음 회원 정보를 복제합니다.
id
username
nickname
activityScore
createDate
modifyDate

비밀번호는 다른 모듈에서 필요하지 않기 때문에 복제하지 않고 빈 값으로 저장합니다.
원본 회원 ID를 그대로 사용하기 때문에 다음과 같이 대응됩니다.
Member id = 4
PostMember id = 4

12. 회원 수정 동기화
회원 활동점수가 변경되면 MemberModifiedEvent를 발행합니다.
Member 활동점수 변경
    ↓
MemberModifiedEvent
    ↓
PostEventListener
    ↓
syncMember()
    ↓
기존 PostMember 갱신

원본과 복제본이 같은 ID를 사용하므로 수정 이벤트가 발생해도 새로운 회원 행이 계속 추가되지 않고 기존 복제 회원 정보가 갱신됩니다.
13. 직접 의존 제거
글과 댓글의 작성자는 원본 Member가 아닌 PostMember를 참조합니다.
Post.author
→ PostMember

PostComment.author
→ PostMember

post 모듈에서는 member 모듈의 Repository나 원본 회원 테이블을 직접 조회하지 않습니다.
모듈 간 통신에는 shared에 위치한 DTO, Event, ApiClient를 사용합니다.
14. 확인 결과
초기 데이터
회원 : 6명
글   : 6개
댓글 : 8개

회원별 글 개수
user1 : 3개
user2 : 2개
user3 : 1개

현재 초기 댓글 작성자 기준 활동점수
user1
글 3개 × 3점 = 9점
댓글 2개 × 1점 = 2점
총 11점

user2
글 2개 × 3점 = 6점
댓글 3개 × 1점 = 3점
총 9점

user3
글 1개 × 3점 = 3점
댓글 3개 × 1점 = 3점
총 6점

15. 확인용 SQL
회원
SELECT *
FROM MEMBER_MEMBER;

복제 회원
SELECT *
FROM POST_MEMBER;

글
SELECT *
FROM POST_POST;

댓글
SELECT *
FROM POST_POST_COMMENT;

원본 회원과 복제 회원의 활동점수 비교
SELECT ID, USERNAME, ACTIVITY_SCORE
FROM MEMBER_MEMBER;

SELECT ID, USERNAME, ACTIVITY_SCORE
FROM POST_MEMBER;

예상 결과
user1 : 11
user2 : 9
user3 : 6

MEMBER_MEMBER와 POST_MEMBER에서 ID, username, nickname, activityScore, 생성/수정 시각이 동일하게 유지됩니다.
16. 재실행 확인
같은 H2 파일 DB를 사용하여 애플리케이션을 다시 실행해도 초기 데이터 생성 조건을 확인하기 때문에 회원, 글, 댓글이 추가로 생성되지 않습니다.
활동점수 역시 기존 글과 댓글 때문에 다시 증가하지 않습니다.
17. 테스트
기본 Spring Context 테스트를 포함합니다.
./gradlew clean test

통합 테스트를 이용한 활동점수 및 복제본 동기화 검증은 선택 요구사항입니다.
EOF
./gradlew clean test && 
git add . && 
git commit -m "docs: complete part1 assignment" && 
git push origin main