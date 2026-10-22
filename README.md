# Seoul City Tour 관리 시스템 (백엔드)

서울시티투어 내부 관리 시스템의 서버(API)입니다.
화면(프론트엔드)은 [seoul-city-tour-front](https://github.com/hdachan/seoul-city-tour-front) 저장소에 있고,
계정 · 권한 · 영업 정산 규칙 같은 전체 설명도 그쪽 README 에 있습니다.

| 항목 | 내용 |
|---|---|
| 기술 | Spring Boot 3.2, Java 17, Spring Security + JWT, JPA (`ddl-auto=update`), MySQL |
| 포트 | 8080 (API 주소는 `/api/...`) |
| 빌드 결과 | `target/seoul-city-tour-1.0.0.jar` |

---

## 1. 설정 파일 (git 에 올리지 않음)

DB 비밀번호 · JWT 키가 들어 있어서 아래 파일은 **저장소에 올리지 않습니다.**
새 PC 에서 작업할 때는 기존 PC 에서 복사해 와야 합니다.

| 파일 | 내용 |
|---|---|
| `src/main/resources/application.properties` | 공통 설정 (포트, JWT 키 · 만료시간, JPA) |
| `src/main/resources/application-local.properties` | 내 PC 의 MySQL 접속 정보 |
| `src/main/resources/application-server.properties` | 운영 서버(Cafe24)의 MySQL 접속 정보 |
| `db_tunnel.bat` | 내 PC 에서 운영 DB 에 접속하기 위한 SSH 터널 |

---

## 2. 로컬에서 실행하기

1. 내 PC 에 MySQL 을 켜고 `seoul_city_tour` DB 를 만들어 둡니다.
   테이블은 서버가 처음 뜰 때 자동으로 만들어집니다 (`ddl-auto=update`).
2. `local` 프로필로 실행합니다.
   - IntelliJ: 실행 설정의 Active profiles 에 `local`
   - 터미널:
     ```bash
     ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
     ```
3. 계정이 하나도 없으면 테스트 계정(admin, sales, guide, dev)이 자동으로 만들어집니다 (`DataInitializer`).

> 8080 포트를 다른 프로그램이 쓰고 있으면 로그인에서 403 이 날 수 있습니다. 다른 서버를 끄고 다시 실행하세요.

---

## 3. 운영 DB 에 접속하기 (SSH 터널)

`db_tunnel.bat` 을 실행해 두면 내 PC 의 **13306 포트**가 운영 서버의 MySQL(3306)로 연결됩니다.
DB 툴에서 `localhost:13306` 으로 접속하면 운영 DB 를 볼 수 있습니다. 창을 닫으면 연결이 끊깁니다.

> 운영 DB 는 실제 데이터입니다. 수정 · 삭제 SQL 은 꼭 백업하고 실행하세요.

---

## 4. 빌드 · 배포

```bash
./mvnw package -DskipTests
```
- `target/seoul-city-tour-1.0.0.jar` 가 만들어집니다.
- IntelliJ 로 서버를 켜둔 상태에서는 `target` 폴더가 잠겨 `clean` 이 실패할 수 있습니다. 서버를 끄거나 `clean` 없이 `package` 만 하세요.
- 만든 jar 를 운영 서버에 올리고 `server` 프로필로 실행합니다.
  ```bash
  java -jar seoul-city-tour-1.0.0.jar --spring.profiles.active=server
  ```

> 프론트와 같이 바뀐 경우 **백엔드를 먼저** 배포하세요.

---

## 5. 구조

```
src/main/java/com/example/seoulcitytour/
├── config/      보안(JWT · 권한 · 입력값 검사 · 요청 제한), 처음 실행 시 기본 데이터
├── controller/  API (auth, admin, dev, record, settlement, ginseng, guide-form, guide-admin, sales-form, sales-admin)
├── entity/      DB 테이블
├── repository/  DB 조회
├── service/     PersonalFuelService (영업 개인주유 정산 · 이월 계산), TabPermissionService (탭 권한 검사)
└── scheduler/   SalesWeekLockScheduler (매일 자정: 지난 주 잠금 · 이번 주 열기)
```

### 권한 검사
- 각 API 는 `@PreAuthorize("@tabPermissionService.hasAccess(authentication, '<탭 id>')")` 로
  **그 탭을 볼 수 있는 역할만** 호출할 수 있습니다.
- 역할별 탭 권한은 프론트의 ⚙️ 개발 탭에서 바꾸며, DB(`TabPermission`)에 저장됩니다.

### 영업 주 잠금
- `SalesMonthLock` 에 사람 · 연 · 월 · 주차별 잠금 여부를 저장합니다 (주차 0 = 달 전체 잠금).
- 기록이 없는 주는 **이번 주만 열림, 나머지는 잠김**으로 봅니다.
- 영업 사원의 저장 · 수정 · 삭제 API 는 해당 날짜의 주가 잠겨 있으면 거절합니다.
