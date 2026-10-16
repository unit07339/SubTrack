# 📋 TASK.md: 구독 관리 웹(SubTrack) 마이크로 태스크 백로그

## 1. 태스크 관리 규칙 (Task Management Rules)

1. **엄격한 단일 진행 원칙**: 동시에 두 개 이상의 태스크를 `[/]`(진행 중) 상태로 둘 수 없습니다.
2. **DoD(Definition of Done) 충족 필수**: 태스크에 지정된 하네스 검증 명령어가 완전히 통과해야만 `[x]`(완료)로 전이할 수 있습니다.
3. **태스크 상태 기호**:
   - `[ ]`: 대기 중 (Pending)
   - `[/]`: 현재 루프에서 진행 중 (In Progress - 최대 1개)
   - `[x]`: 하네스 검증 통과 및 완료 (Done)
   - `[-]`: 스코프 제외 또는 보류 (Skipped)

---

## 2. 단계별 마이크로 태스크 목록

### Phase 0: 프로젝트 부트스트래핑 및 하네스 환경 세팅
- [x] **TASK-001**: Java 21 기반 `pom.xml`, Maven 래퍼/하네스 스크립트(`harness.ps1`) 및 기본 패키지 디렉터리 구성
  - **DoD**: `pom.xml` 컴파일 및 `.\harness.ps1` 실행 준비 완료
  - **검증**: `.\harness.ps1 compile`
- [x] **TASK-002**: 하네스 테스트 러너(JUnit 5, AssertJ, Jackson) 검증 및 초기 더미 테스트 통과 세팅
  - **DoD**: `.\harness.ps1 verify` 실행 시 0 에러로 정상 종료
  - **검증**: `.\harness.ps1 verify`

---

### Phase 1: 구독 도메인 모델, 계산기 및 하네스 저장소
- [x] **TASK-010**: 구독 엔티티(`Subscription`), Enum(`BillingCycle`, `Category`, `SubscriptionStatus`), 프리셋 정의
  - **DoD**: Jackson 직렬화/역직렬화 및 유효성 검사 단위 테스트 통과
  - **검증**: `.\harness.ps1 quick`
- [x] **TASK-011**: 날짜 및 결제액 계산기 (`BillingCalculator`: D-Day 계산, 결제주기 환산, 다음 결제일 계산 로직) 구현
  - **DoD**: 윤달, 31일 말일 결제 예외 케이스를 포함한 단위 테스트 100% 통과
  - **검증**: `.\harness.ps1 quick`
- [x] **TASK-012**: 저장소 인터페이스(`SubscriptionRepository`) 및 인메모리 하네스 저장소 구현
  - **DoD**: CRUD(등록, 조회, 목록, 수정, 상태변경, 삭제) 단위 테스트 100% 통과
  - **검증**: `.\harness.ps1 quick`

---

### Phase 2: 서버리스 FaaS API 핸들러 구현
- [x] **TASK-020**: 헬스체크(`HealthHandler`) 및 프리셋 목록(`PresetsHandler`) 서버리스 핸들러 구현
  - **DoD**: HTTP 200 반환 및 JSON Envelope 규격 준수 검증
  - **검증**: `.\harness.ps1 api`
- [x] **TASK-021**: 구독 신규 등록 및 단건 조회 서버리스 핸들러 구현
  - **DoD**: 잘못된 JSON 입력값 400 에러, 정상 등록 시 201 및 다음 결제일 자동 계산 검증
  - **검증**: `.\harness.ps1 api`
- [x] **TASK-022**: 구독 목록, 수정, 삭제, 상태 토글 서버리스 핸들러 구현
  - **DoD**: 카테고리/상태 필터링 조회, 수정 반영, 삭제 후 404 반환 검증
  - **검증**: `.\harness.ps1 api`
- [x] **TASK-023**: 대시보드 지출 통계 및 요약 집계 핸들러(`AnalyticsSummaryHandler`) 구현
  - **DoD**: 이번 달 지출 총액, D-7 임박 목록, 카테고리별 비중 반환 검증
  - **검증**: `.\harness.ps1 api`

---

### Phase 3: 프론트엔드 반응형 웹 SPA 구현
- [x] **TASK-030**: 반응형 앱 레이아웃 및 상단 네비게이션 헤더 구현 (`src/main/resources/static/index.html`)
  - **DoD**: 모바일/데스크톱 반응형 뷰포트 및 탭 전환 마크업/스타일 완성
  - **검증**: `.\harness.ps1 verify`
- [x] **TASK-031**: 대시보드 뷰 UI 및 서버리스 API 비동기 연동 (지표 카드, D-Day 임박 위젯, 요약 차트)
  - **DoD**: 대시보드 데이터 페칭 및 렌더링 확인
  - **검증**: `.\harness.ps1 verify`
- [x] **TASK-032**: 구독 목록 뷰 및 신규 등록/수정 모달 구현 (프리셋 퀵 선택 지원)
  - **DoD**: 구독 카드 그리드 렌더링 및 모달 폼을 통한 등록/삭제/상태변경 기능 동작
  - **검증**: `.\harness.ps1 verify`
- [x] **TASK-033**: 결제 캘린더 뷰 및 지출 분석 뷰 구현 (월별 캘린더 그리드, 카테고리 도넛 차트)
  - **DoD**: 캘린더 날짜별 결제 뱃지 및 카테고리별 지출 비율 시각화 확인
  - **검증**: `.\harness.ps1 verify`

---

### Phase 4: 전체 하네스 통합 검증 & 패키징
- [x] **TASK-040**: 클라이언트 ➔ 서버리스 FaaS ➔ 데이터 저장소 E2E 계약 검증 테스트
  - **DoD**: `E2EContractTest.java` 시나리오 100% PASS
  - **검증**: `.\harness.ps1 verify`
- [x] **TASK-041**: 최종 JAR 패키징 및 하네스 골든 패스 완료
  - **DoD**: `.\harness.ps1 verify` 실행 시 0 에러 확인
  - **검증**: `.\harness.ps1 verify`
