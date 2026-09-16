# AutoBlog 운영에 필요한 설정값 정리

이 문서는 AutoBlog 관리자 웹을 실행하고 실제 워드프레스/GA4/AI 자동화를 사용하기 위해 필요한 설정값을 정리한 문서입니다.
보안상 실제 비밀번호, 토큰, API Key 값은 문서에 직접 적지 않습니다.

## 1. Java 웹 애플리케이션 기본 환경

- Java: 1.8
- WAS: Tomcat 9 계열 권장
- DB: MariaDB
- 빌드 방식: Maven 미사용, `WEB-INF/lib` 수동 JAR 방식
- Spring 설정: `src/main/resources/ApplicationContext.xml`, `src/main/webapp/WEB-INF/spring-servlet.xml`

## 2. MariaDB 연결 정보

위치:

- `src/main/resources/ApplicationContext.xml`

필요값:

- DB host
- DB port
- DB name
- DB username
- DB password

현재 구조에서는 `dataSource` bean에 직접 들어갑니다.
운영 배포 시에는 XML 직접 하드코딩보다 외부 properties 파일이나 환경변수 분리를 권장합니다.

## 3. 워드프레스 사이트 연결 정보

저장 위치:

- MariaDB `wordpress_sites` 테이블
- 설정 화면 `/settings`

필요값:

- 사이트 이름
- 사이트 URL
- Posts API URL
  - 예: `https://example.com/wp-json/wp/v2/posts`
- 워드프레스 사용자명
- WordPress Application Password
- 관리자 이메일
- 기본 발행 상태

주의:

- Application Password는 워드프레스 사용자 프로필에서 발급합니다.
- 화면에는 공백이 포함되어 보일 수 있지만, 인증 시에는 코드에서 공백을 제거합니다.
- 카테고리 발행은 워드프레스 카테고리 ID를 REST API `categories: [id]` 형태로 보냅니다.

## 4. OpenAI API 설정

위치:

- Python 프로젝트 설정: `C:\ProjectAll\blog_site\automation\.env`
- 실행 스크립트: `C:\ProjectAll\blog_site\automation\blog_automation.py`

필요값:

- `OPENAI_API_KEY`
- `OPENAI_MODEL`

역할:

- 키워드 수집
- 블로그 글 생성
- SEO/품질 검증
- 검증 이슈 반영 재작성
- 맞춤법/문장 교열
- 짧은 본문 자동 확장

주의:

- Python stdout은 Java에서 UTF-8로 읽도록 고정되어 있습니다.
- `.env` 파일은 UTF-8로 저장해야 합니다.

## 5. 네이버 DataLab API 설정

위치:

- `C:\ProjectAll\blog_site\automation\.env`

필요값:

- `NAVER_CLIENT_ID`
- `NAVER_CLIENT_SECRET`
- `NAVER_START_DATE`
- `NAVER_END_DATE`

역할:

- 키워드 분석 페이지에서 검색 추세 조회

주의:

- DataLab 값은 절대 검색량이 아니라 상대 비율입니다.

## 6. GA4 통계 연결 정보

위치:

- `src/main/resources/ApplicationContext.xml`
- `ga4AnalyticsService` bean

필요값:

- GA4 Property ID
- Google OAuth Client ID
- Google OAuth Client Secret
- Refresh Token

역할:

- 방문자 수
- 조회수
- 평균 체류 시간
- 이탈률
- 채널/디바이스 통계
- 페이지별 조회수 매칭

주의:

- GA4 호출은 외부 API라 5분 캐시가 적용되어 있습니다.
- 실제 글별 조회수는 GA4 `pagePath + screenPageViews`를 워드프레스 글 URL과 매칭합니다.

## 7. 예약 자동화 설정

저장 위치:

- MariaDB `wordpress_sites` 테이블
- 설정 화면 `/settings`

주요 컬럼:

- `schedule_enabled`
- `schedule_start_time`
- `scheduled_keyword_count`
- `scheduled_post_count`
- `schedule_category`
- `last_scheduled_run_date`

동작 방식:

1. 서버가 켜지면 `ScheduledAutomationServiceImpl`이 시작됩니다.
2. 1분마다 활성 사이트의 예약 설정을 확인합니다.
3. 설정 시간이 지났고 오늘 아직 실행하지 않았으면 실행합니다.
4. 워드프레스 카테고리 기준으로 키워드를 수집합니다.
5. 수집된 키워드로 글을 생성합니다.
6. 자동 발행 ON이면 워드프레스에 바로 발행합니다.
7. 자동 발행 OFF이면 생성 글 검토에 저장합니다.

현재 제한:

- 현재는 활성 사이트 기준으로 실행됩니다.
- 사이트별 서로 다른 예약 실행은 다음 확장 단계에서 분리할 수 있습니다.

## 8. 설정 화면에서 실제 동작하는 주요 기능

- 워드프레스 연결 테스트
- 사이트 전환
- 새 사이트 추가
- 자동 키워드 수집 ON/OFF
- 자동 본문 생성 ON/OFF
- 자동 발행 ON/OFF
- 예약 자동 실행 ON/OFF
- 예약 시작 시간/수집 개수/글 작성 개수 설정
- 워드프레스 카테고리 기반 키워드 수집
- SEO 제목 접미사
- 메타 설명 템플릿
- 내부 링크 자동 추가
- 알림 이메일/발행 알림 설정값 저장

## 9. 아직 보강 가능한 부분

- 실제 SMTP 이메일 발송
- 대표 이미지 자동 생성 및 워드프레스 미디어 업로드
- 사이트별 독립 예약 실행
- GA4 통계 강제 새로고침 버튼
- 설정값 외부 properties/env 분리

## 10. 보안 주의

다음 값은 절대 Git이나 공개 문서에 올리면 안 됩니다.

- WordPress Application Password
- OpenAI API Key
- Naver Client Secret
- Google OAuth Client Secret
- Google Refresh Token
- MariaDB Password
