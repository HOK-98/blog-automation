# Keyword to WordPress Automation

이 프로젝트는 아래 4단계만 자동화합니다.

```text
키워드 수집
  -> 블로그 초안 생성
  -> 2차 검증
  -> 워드프레스 업로드
```

제외한 단계:

- 검색량/난이도 분석
- 제목 추천 전용 단계
- 이미지 자동 생성

## 설치

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

`.env`에 OpenAI API 키와 워드프레스 정보를 입력하세요.

워드프레스 비밀번호는 일반 로그인 비밀번호가 아니라 `사용자 > 프로필 > 애플리케이션 비밀번호`에서 만든 값을 사용합니다.

## 실행

```powershell
python blog_automation.py
```

기본값은 안전하게 테스트하는 설정입니다.

```env
DRY_RUN=true
WORDPRESS_STATUS=draft
```

실제 워드프레스 초안 업로드를 하려면:

```env
DRY_RUN=false
WORDPRESS_STATUS=draft
```

공개 발행까지 자동으로 하려면:

```env
DRY_RUN=false
WORDPRESS_STATUS=publish
```

## 키워드 수집

`KEYWORD_SOURCE=gpt`

- 블로그 주제, 독자, 시드 키워드를 바탕으로 글감 후보를 만듭니다.

`KEYWORD_SOURCE=naver`

- Naver DataLab API로 설정한 시드 키워드들의 추세를 비교합니다.
- DataLab은 전체 실시간 인기검색어를 그대로 주는 API가 아니므로 `SEED_KEYWORDS`가 필요합니다.

## 워드프레스 업로드

업로드 시 자동 처리되는 값:

- 제목
- 본문 HTML
- 요약
- slug
- 태그
- 카테고리

태그와 카테고리는 같은 이름이 있으면 재사용하고, 없으면 새로 만든 뒤 글에 연결합니다.

## 주의

- GPT 생성 글은 최신 사실을 보장하지 않습니다.
- 그래서 업로드 전 2차 검증 단계를 둡니다.
- 민감한 주제나 최신 뉴스성 글은 사람이 한 번 더 보는 운영을 권장합니다.
