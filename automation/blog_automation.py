from __future__ import annotations

# Standard library imports.
import base64
import json
import os
import re
import sys
from dataclasses import dataclass
from datetime import date, timedelta
from typing import Any

# Third-party libraries.
import requests
from dotenv import load_dotenv
from openai import OpenAI


@dataclass(frozen=True)
class Config:
    """Runtime settings loaded from the .env file.

    The script keeps all user-editable settings in .env so the code can stay
    stable while API keys, WordPress settings, topic settings, and safety
    thresholds change between runs.
    """

    openai_api_key: str
    openai_model: str
    keyword_source: str
    blog_niche: str
    blog_audience: str
    seed_keywords: list[str]
    keyword_count: int
    naver_client_id: str
    naver_client_secret: str
    naver_start_date: str
    naver_end_date: str
    wordpress_base_url: str
    wordpress_username: str
    wordpress_app_password: str
    wordpress_status: str
    posts_per_run: int
    min_verification_score: int
    article_min_chars: int
    article_max_chars: int
    max_revision_rounds: int
    max_expansion_rounds: int
    dry_run: bool


def env_bool(name: str, default: bool) -> bool:
    """Read a boolean-like .env value.

    Supported true values are intentionally broad because users often write
    true/yes/1/on interchangeably in config files.
    """

    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


def env_int(name: str, default: int) -> int:
    """Read an integer .env value and fail with a clear message if invalid."""

    value = os.getenv(name)
    if not value:
        return default
    try:
        return int(value)
    except ValueError as exc:
        raise ValueError(f"{name} must be an integer") from exc


def load_config() -> Config:
    """Load .env settings into a Config object.

    Defaults are conservative: one post per run, draft publishing, dry-run mode,
    and one revision/expansion attempt.
    """

    load_dotenv()
    today = date.today()
    default_start = today - timedelta(days=30)
    seed_keywords = [
        keyword.strip()
        for keyword in os.getenv("SEED_KEYWORDS", "").split(",")
        if keyword.strip()
    ]
    return Config(
        openai_api_key=os.getenv("OPENAI_API_KEY", ""),
        openai_model=os.getenv("OPENAI_MODEL", "gpt-5.3-chat-latest"),
        keyword_source=os.getenv("KEYWORD_SOURCE", "gpt").strip().lower(),
        blog_niche=os.getenv("BLOG_NICHE", "IT 트렌드"),
        blog_audience=os.getenv("BLOG_AUDIENCE", "한국의 일반 독자"),
        seed_keywords=seed_keywords,
        keyword_count=env_int("KEYWORD_COUNT", 5),
        naver_client_id=os.getenv("NAVER_CLIENT_ID", ""),
        naver_client_secret=os.getenv("NAVER_CLIENT_SECRET", ""),
        naver_start_date=os.getenv("NAVER_START_DATE", default_start.isoformat()),
        naver_end_date=os.getenv("NAVER_END_DATE", today.isoformat()),
        wordpress_base_url=os.getenv("WORDPRESS_BASE_URL", "").rstrip("/"),
        wordpress_username=os.getenv("WORDPRESS_USERNAME", ""),
        wordpress_app_password=os.getenv("WORDPRESS_APP_PASSWORD", ""),
        wordpress_status=os.getenv("WORDPRESS_STATUS", "draft"),
        posts_per_run=env_int("POSTS_PER_RUN", 1),
        min_verification_score=env_int("MIN_VERIFICATION_SCORE", 80),
        article_min_chars=env_int("ARTICLE_MIN_CHARS", 1600),
        article_max_chars=env_int("ARTICLE_MAX_CHARS", 2200),
        max_revision_rounds=env_int("MAX_REVISION_ROUNDS", 1),
        max_expansion_rounds=env_int("MAX_EXPANSION_ROUNDS", 1),
        dry_run=env_bool("DRY_RUN", True),
    )


def require(value: str, name: str) -> None:
    """Stop early when a required setting is missing."""

    if not value:
        raise RuntimeError(f"{name} is required. Add it to .env.")


class GptClient:
    """Small wrapper around the OpenAI Chat Completions API.

    Every GPT call in this project expects JSON. Centralizing the call here
    keeps JSON parsing, error handling, and token usage tracking in one place.
    """

    def __init__(self, config: Config) -> None:
        require(config.openai_api_key, "OPENAI_API_KEY")
        self.model = config.openai_model
        self.client = OpenAI(api_key=config.openai_api_key)
        self.prompt_tokens = 0
        self.completion_tokens = 0
        self.total_tokens = 0

    def usage_summary(self) -> dict[str, int]:
        """Return accumulated token usage for the current run."""

        return {
            "prompt_tokens": self.prompt_tokens,
            "completion_tokens": self.completion_tokens,
            "total_tokens": self.total_tokens,
        }

    def json_chat(self, system: str, user: str) -> dict[str, Any]:
        """Ask GPT for a JSON object and parse the response."""

        response = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
            response_format={"type": "json_object"},
        )

        # Track usage when the API returns it. This is useful for cost checks.
        usage = getattr(response, "usage", None)
        if usage is not None:
            self.prompt_tokens += int(getattr(usage, "prompt_tokens", 0) or 0)
            self.completion_tokens += int(getattr(usage, "completion_tokens", 0) or 0)
            self.total_tokens += int(getattr(usage, "total_tokens", 0) or 0)

        content = response.choices[0].message.content or "{}"
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"GPT returned invalid JSON: {content}") from exc
        if not isinstance(parsed, dict):
            raise RuntimeError("GPT response must be a JSON object.")
        return parsed

    def json_web_chat(self, system: str, user: str) -> dict[str, Any]:
        """Fact-check with live web search and return a JSON object."""

        response = self.client.responses.create(
            model=self.model,
            tools=[
                {
                    "type": "web_search_preview",
                    "search_context_size": "medium",
                    "user_location": {
                        "type": "approximate",
                        "country": "KR",
                        "timezone": "Asia/Seoul",
                    },
                }
            ],
            input=[
                {"role": "system", "content": system},
                {"role": "user", "content": user},
            ],
        )

        usage = getattr(response, "usage", None)
        if usage is not None:
            input_tokens = int(getattr(usage, "input_tokens", 0) or 0)
            output_tokens = int(getattr(usage, "output_tokens", 0) or 0)
            self.prompt_tokens += input_tokens
            self.completion_tokens += output_tokens
            self.total_tokens += input_tokens + output_tokens

        content = (getattr(response, "output_text", "") or "").strip()
        if content.startswith("```"):
            content = re.sub(r"^```(?:json)?\s*|\s*```$", "", content, flags=re.DOTALL)
        try:
            parsed = json.loads(content)
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"GPT web verification returned invalid JSON: {content}") from exc
        if not isinstance(parsed, dict):
            raise RuntimeError("GPT web verification response must be a JSON object.")
        return parsed


class KeywordProvider:
    """Collect keyword candidates from GPT or Naver DataLab."""

    def __init__(self, config: Config, gpt: GptClient) -> None:
        self.config = config
        self.gpt = gpt

    def collect(self) -> list[str]:
        """Dispatch to the configured keyword source."""

        if self.config.keyword_source == "naver":
            return self._collect_from_naver()
        if self.config.keyword_source != "gpt":
            raise RuntimeError("KEYWORD_SOURCE must be either 'gpt' or 'naver'.")
        return self._collect_from_gpt()

    def _collect_from_gpt(self) -> list[str]:
        """Ask GPT for realistic search keywords based on the blog settings."""

        seed = ", ".join(self.config.seed_keywords) or "없음"
        payload = self.gpt.json_chat(
            "You are a Korean blog keyword researcher. Return only valid JSON.",
            (
                "한국어 블로그용 키워드 후보를 만들어줘.\n"
                f"블로그 주제: {self.config.blog_niche}\n"
                f"대상 독자: {self.config.blog_audience}\n"
                f"시드 키워드: {seed}\n"
                f"필요 개수: {self.config.keyword_count}\n"
                "조건:\n"
                "- 사람들이 실제로 검색할 법한 구체적인 표현 우선\n"
                "- 너무 넓은 단어보다 글 한 편으로 답할 수 있는 검색어 우선\n"
                "- 별도 SEO 점수 계산이나 제목 추천은 하지 말 것\n"
                "- 키워드는 제목이 아니라 검색어여야 함\n"
                "- '실전 가이드', '완벽 가이드', '총정리', '한눈에 정리', 'A to Z', '최신 트렌드' 같은 제목형 표현 금지\n"
                'JSON 형식: {"keywords": ["키워드1", "키워드2"]}'
            ),
        )
        return normalize_keywords(payload.get("keywords", []), self.config.keyword_count)

    def _collect_from_naver(self) -> list[str]:
        """Use Naver DataLab to rank configured seed keywords by trend ratio.

        Naver DataLab does not provide a global real-time search-rank API here.
        It compares the keyword groups we send, so SEED_KEYWORDS is required.
        """

        require(self.config.naver_client_id, "NAVER_CLIENT_ID")
        require(self.config.naver_client_secret, "NAVER_CLIENT_SECRET")
        if not self.config.seed_keywords:
            raise RuntimeError("SEED_KEYWORDS is required for KEYWORD_SOURCE=naver.")

        keyword_groups = [
            {"groupName": keyword, "keywords": [keyword]}
            for keyword in self.config.seed_keywords[:5]
        ]
        response = requests.post(
            "https://openapi.naver.com/v1/datalab/search",
            headers={
                "X-Naver-Client-Id": self.config.naver_client_id,
                "X-Naver-Client-Secret": self.config.naver_client_secret,
                "Content-Type": "application/json",
            },
            json={
                "startDate": self.config.naver_start_date,
                "endDate": self.config.naver_end_date,
                "timeUnit": "date",
                "keywordGroups": keyword_groups,
            },
            timeout=20,
        )
        response.raise_for_status()
        data = response.json()

        # Sum each keyword group's ratios over the selected period.
        scored: list[tuple[str, float]] = []
        for result in data.get("results", []):
            score = sum(item.get("ratio", 0.0) for item in result.get("data", []))
            scored.append((result.get("title", ""), score))
        scored.sort(key=lambda item: item[1], reverse=True)
        return normalize_keywords([keyword for keyword, _ in scored], self.config.keyword_count)


def normalize_keywords(raw_keywords: Any, limit: int) -> list[str]:
    """Clean, deduplicate, and limit keyword candidates."""

    if not isinstance(raw_keywords, list):
        return []
    seen: set[str] = set()
    keywords: list[str] = []
    for item in raw_keywords:
        keyword = clean_search_keyword(str(item).strip())
        key = keyword.lower()
        if keyword and key not in seen:
            seen.add(key)
            keywords.append(keyword)
        if len(keywords) >= limit:
            break
    return keywords


def clean_search_keyword(keyword: str) -> str:
    """Remove title-like suffixes from keyword candidates."""

    keyword = re.sub(r"\s+", " ", str(keyword or "")).strip()
    suffixes = [
        r"(실전\s*)?가이드",
        r"완벽\s*가이드",
        r"초보자?\s*가이드",
        r"총정리",
        r"한눈에\s*정리",
        r"A\s*to\s*Z",
        r"최신\s*트렌드",
        r"전망",
    ]
    for suffix in suffixes:
        keyword = re.sub(rf"[\s:：\-–—|]+{suffix}$", "", keyword, flags=re.IGNORECASE).strip()
    return keyword


def clean_generated_title(title: str, keyword: str) -> str:
    """Normalize GPT-generated titles so the original keyword stays intact."""

    cleaned = clean_search_keyword(title)
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    keyword = clean_search_keyword(keyword)
    if keyword and keyword not in cleaned:
        return keyword
    return cleaned or keyword or title.strip()


def normalize_article_for_keyword(article: dict[str, Any], keyword: str) -> dict[str, Any]:
    """Apply final article cleanup that should happen after every GPT pass."""

    normalized = dict(article)
    normalized["title"] = clean_generated_title(str(normalized.get("title", "")), keyword)
    return normalized


class ArticlePipeline:
    """Generate, review, revise, proofread, and expand blog articles."""

    def __init__(self, config: Config, gpt: GptClient) -> None:
        self.config = config
        self.gpt = gpt

    def generate(self, keyword: str) -> dict[str, Any]:
        """Create the first WordPress-ready blog draft."""

        return self.gpt.json_chat(
            "You are a careful Korean blog editor. Return only valid JSON.",
            (
                "Write a Korean blog draft for WordPress.\n"
                f"Keyword: {keyword}\n"
                f"Blog niche: {self.config.blog_niche}\n"
                f"Target audience: {self.config.blog_audience}\n"
                "Requirements:\n"
                "- Write in natural Korean for human readers.\n"
                f"- Write a concise but useful article. The visible Korean text in content_html should be about {self.config.article_min_chars} to {self.config.article_max_chars} Korean characters.\n"
                "- Do not produce a thin summary, but do not make the article unnecessarily long. Keep only the details that help the reader decide or understand quickly.\n"
                "- Solve the searcher's intent first.\n"
                "- Include the exact primary keyword naturally in the title.\n"
                "- Do not change the primary keyword into a different keyword.\n"
                "- Keep the title close to the actual search phrase. Do not add generic title-style suffixes.\n"
                "- Mention the keyword and the reader's main question naturally in the opening paragraph.\n"
                "- Before writing, choose one fitting article style: personal-experience style, comparison, beginner explanation, analysis, checklist, or case-based explanation. Avoid making every article sound like a generic how-to guide.\n"
                "- Make the article structure, pacing, examples, and transitions match that chosen style.\n"
                "- Build h2/h3 headings around questions people are likely to search for.\n"
                "- Do not force the same template every time. Do not include FAQ by default; include it only when the keyword is clearly question-heavy and FAQ genuinely helps.\n"
                "- Do not add repeated boilerplate sections such as 함께 읽으면 좋은 글, 관련 글 추천, 마무리, 결론, 실전 가이드, 체크리스트 unless they are truly necessary for this exact topic.\n"
                "- End naturally in 2 to 4 sentences instead of always using a formal conclusion heading.\n"
                "- Return the body as WordPress-friendly HTML.\n"
                "- Use h2/h3 headings, lists, and short paragraphs.\n"
                "- Write the excerpt like a compelling meta description, about 120 to 160 Korean characters.\n"
                "- Avoid keyword stuffing; mix in related terms and natural phrasing.\n"
                "- Vary sentence length and rhythm so the article feels human-written, not templated.\n"
                "- Add concrete examples and situation-specific explanations where they help, but keep each section compact.\n"
                "- Include 2 to 3 specific, useful details that a generic article would usually miss.\n"
                "- When giving tips, explain why they work and when they may not work.\n"
                "- Avoid generic intros, repeated wording, and overused filler phrases.\n"
                "- Do not overuse the Korean words '실전', '실무', '바로 적용', or '가이드'. Use them only when truly necessary, and never as a repeated framing phrase.\n"
                "- Do not state uncertain statistics or dates as facts.\n"
                "- Avoid exaggerated advertising language.\n"
                "- Follow Google AdSense policies.\n"
                "- Make the article original and genuinely useful, not low-value generated text.\n"
                "- Do not include illegal, sexual, dangerous, hateful, or copyright-infringing content.\n"
                "- Do not encourage ad clicks.\n"
                'JSON format: {"title": "...", "slug": "...", "excerpt": "...", '
                '"content_html": "...", "tags": ["..."], "categories": ["..."]}'
            ),
        )


    def quick_review(self, keyword: str, article: dict[str, Any]) -> dict[str, Any]:
        """Cheap non-web review for structure, style, SEO, and obvious risk before final fact-check."""

        return self.gpt.json_chat(
            "You are a strict Korean blog editor. Return only valid JSON.",
            (
                "Review the Korean blog draft below without web search. This is a cheap pre-review, not the final factual approval.\n"
                f"Keyword: {keyword}\n"
                f"Title: {article.get('title', '')}\n"
                f"Excerpt: {article.get('excerpt', '')}\n"
                f"HTML body: {article.get('content_html', '')}\n"
                "Review criteria:\n"
                "- Does it answer the search intent clearly and early?\n"
                "- Does the title contain the exact primary keyword without changing the keyword?\n"
                "- Is the article too generic, repetitive, template-like, or shallow?\n"
                "- Does it avoid boilerplate sections such as 함께 읽으면 좋은 글, 관련 글 추천, 마무리, 결론, 실전 가이드 unless truly needed?\n"
                f"- Is the visible Korean text likely within {self.config.article_min_chars} to {self.config.article_max_chars} characters?\n"
                "- Does the writing feel natural and human rather than mechanical?\n"
                "- Are there obvious unsupported numbers, dates, prices, policy claims, or version-sensitive statements that should be softened before web fact-checking?\n"
                "- Do not claim final factual approval because this step has no web search.\n"
                "- Set critical_factual_error true only for obvious internal contradictions or clearly risky claims that must be rewritten before final verification.\n"
                'JSON format: {"approved": true, "critical_factual_error": false, "score": 0, "issues": ["..."], "revision_notes": "...", "sources": []}'
            ),
        )
    def verify(self, keyword: str, article: dict[str, Any]) -> dict[str, Any]:
        """Review the draft before publishing or revision."""

        return self.gpt.json_web_chat(
            "You are a strict Korean editor and web fact-check reviewer. Search the live web and return only valid JSON.",
            (
                "Review the Korean blog draft below using current web sources.\n"
                f"Current date: {date.today().isoformat()}\n"
                f"Keyword: {keyword}\n"
                f"Title: {article.get('title', '')}\n"
                f"Excerpt: {article.get('excerpt', '')}\n"
                f"HTML body: {article.get('content_html', '')}\n"
                "Web research rules:\n"
                "- Search before judging factual claims. Do not rely only on model memory.\n"
                "- Prefer official product documentation, official help centers, government or public institutions, and original announcements.\n"
                "- Use reputable secondary sources only when an official source is unavailable.\n"
                "- Check publication or update dates and distinguish legacy systems from current systems.\n"
                "- Include 1 to 5 directly relevant source URLs in sources.\n"
                "- If an official source page clearly provides a directly usable image URL that helps explain the article, include it in official_images.\n"
                "- official_images must contain only images from official product/help/government/public-institution pages or official press/media pages. Do not use random blog, news, CDN, social, or search-result images.\n"
                "- If licensing or source ownership is unclear, return an empty official_images array.\n"
                "- Do not invent image URLs. Use only direct http/https image URLs found on official source pages.\n"
                "Review criteria:\n"
                "- Does it answer search intent clearly?\n"
                "- Is the primary keyword used naturally in the title, opening paragraph, and major headings?\n"
                "- Does the title contain the exact primary keyword without changing the keyword?\n"
                "- Does the title stay close to the actual search phrase without generic title-style suffixes?\n"
                "- Does it avoid keyword stuffing while using related expressions naturally?\n"
                "- Is the excerpt strong enough to work as a search result description?\n"
                "- Does the article feel like it follows one coherent human writing style rather than a repeated template?\n"
                "- Are the structure, pacing, and examples appropriate for the chosen style?\n"
                "- Is FAQ omitted unless it is genuinely useful for this keyword?\n"
                "- Does it avoid repeated boilerplate sections such as 함께 읽으면 좋은 글, 관련 글 추천, 마무리, 결론, 실전 가이드, and generic checklist sections?\n"
                f"- Is the visible Korean text reasonably within {self.config.article_min_chars} to {self.config.article_max_chars} characters unless the topic clearly needs more?\n"
                "- Are there factual risks, date mistakes, or unsupported claims?\n"
                "- Treat policies, prices, product limits, software behavior, legal rules, schedules, and version-dependent instructions as freshness-sensitive facts.\n"
                "- If the draft mixes an old policy with a current policy, mark critical_factual_error true and approved false.\n"
                "- Never approve a freshness-sensitive claim merely because it sounds plausible. If it cannot be verified confidently, require removal or cautious wording.\n"
                "- Is the article detailed enough, or is it too short and shallow?\n"
                "- Does each major section include concrete examples, context, and topic-specific detail?\n"
                "- Does it avoid vague advice that could apply to any topic?\n"
                "- Is the tone free of exaggeration?\n"
                "- Does it feel human-written, with natural rhythm and limited boilerplate?\n"
                "- Does it avoid repeatedly using words like '실전', '실무', '바로 적용', and '가이드'?\n"
                "- Is it genuinely useful to readers rather than low-value generated text?\n"
                "- Is it safe for AdSense and free of ad-click encouragement?\n"
                "- Is the HTML structure suitable for WordPress publishing?\n"
                'JSON format: {"approved": true, "critical_factual_error": false, "score": 0, "issues": ["..."], "revision_notes": "...", "sources": [{"title": "...", "url": "..."}], "official_images": [{"image_url": "https://...", "source_url": "https://...", "source_title": "...", "caption": "...", "alt": "...", "license": "official source"}]}'
            ),
        )

    def revise(self, keyword: str, article: dict[str, Any], verification: dict[str, Any]) -> dict[str, Any]:
        """Revise an article using the previous verification result."""

        issues = verification.get("issues", [])
        revision_notes = verification.get("revision_notes", "")
        return self.gpt.json_chat(
            "You are a senior Korean blog editor. Return only valid JSON.",
            (
                "Revise the Korean draft using the review feedback below.\n"
                f"Keyword: {keyword}\n"
                f"Current title: {article.get('title', '')}\n"
                f"Current excerpt: {article.get('excerpt', '')}\n"
                f"Current HTML body: {article.get('content_html', '')}\n"
                f"Issues: {json.dumps(issues, ensure_ascii=False)}\n"
                f"Revision notes: {revision_notes}\n"
                "Revision rules:\n"
                "- Resolve every factual, policy, version, date, price, limit, and software-behavior issue in the article itself.\n"
                "- When old and current systems are mixed, remove the obsolete instructions and rewrite the affected section around the current system.\n"
                "- If the current fact cannot be established confidently, remove the precise claim or explicitly state that readers should confirm it on the official service page.\n"
                "- Make the title, opening paragraph, and major headings SEO-friendly while keeping keyword use natural.\n"
                "- Keep the exact primary keyword in the title and do not replace it with a different keyword.\n"
                "- Remove generic title-style suffixes and keep the title close to the actual search phrase.\n"
                "- Reorganize the article so it answers the searcher's real question early.\n"
                "- Keep one coherent writing style and remove template-like sections that do not add value.\n"
                "- Remove FAQ unless it is genuinely useful for this keyword.\n"
                "- Remove repeated boilerplate sections such as 함께 읽으면 좋은 글, 관련 글 추천, 마무리, 결론, 실전 가이드, and generic checklist sections unless they are essential.\n"
                "- If a formal conclusion is unnecessary, replace it with a short natural ending.\n"
                "- Improve the excerpt into a clear, clickable search-result description.\n"
                "- Reduce keyword repetition and use related expressions naturally.\n"
                "- Remove or soften weak claims, exaggeration, and unsupported freshness claims.\n"
                f"- Keep the revised visible Korean text close to {self.config.article_min_chars} to {self.config.article_max_chars} characters. If it is longer, shorten repetitive parts first.\n"
                "- Expand only sections that are too short or generic with useful context, examples, and topic-specific explanation.\n"
                "- Add concrete examples or specific advice only when they improve reader value.\n"
                "- Explain not only what to do, but why it matters and what to watch out for.\n"
                "- Make the prose feel more human: vary sentence length, avoid repetitive transitions, and reduce boilerplate.\n"
                "- Remove repeated framing words such as '실전', '실무', '바로 적용', and '가이드' unless they are essential to the sentence.\n"
                "- Preserve the original topic and search intent.\n"
                "- Keep the body in WordPress-friendly HTML.\n"
                "- Return a polished article, not comments about what should be changed.\n"
                'JSON format: {"title": "...", "slug": "...", "excerpt": "...", '
                '"content_html": "...", "tags": ["..."], "categories": ["..."]}'
            ),
        )

    def proofread(self, article: dict[str, Any]) -> dict[str, Any]:
        """Polish language without changing the topic or adding new claims."""

        return self.gpt.json_chat(
            "You are a meticulous Korean copy editor. Return only valid JSON.",
            (
                "Proofread the Korean article below before publication.\n"
                f"Title: {article.get('title', '')}\n"
                f"Excerpt: {article.get('excerpt', '')}\n"
                f"HTML body: {article.get('content_html', '')}\n"
                "Proofreading rules:\n"
                "- Correct Korean spelling, spacing, particles, grammar, and awkward phrasing.\n"
                "- Shorten redundant or repetitive sentences, especially boilerplate endings and unnecessary FAQ-style parts.\n"
                "- Remove translation-like wording and overly mechanical phrasing when possible.\n"
                "- Keep the meaning, SEO intent, headings, and HTML structure intact.\n"
                "- Do not add new claims, facts, dates, or unsupported examples.\n"
                "- Keep title, excerpt, tags, categories, and body in Korean where appropriate.\n"
                "- Return the polished article only.\n"
                'JSON format: {"title": "...", "slug": "...", "excerpt": "...", '
                '"content_html": "...", "tags": ["..."], "categories": ["..."]}'
            ),
        )

    def expand(self, keyword: str, article: dict[str, Any], minimum_chars: int) -> dict[str, Any]:
        """Expand a short article while preserving the same search intent."""

        current_chars = visible_text_length(article.get("content_html", ""))
        return self.gpt.json_chat(
            "You are a concise Korean blog editor. Return only valid JSON.",
            (
                "The Korean blog article below is too short. Expand it into a concise, useful article without making it overly long.\n"
                f"Keyword: {keyword}\n"
                f"Minimum visible Korean text length: {minimum_chars} characters\n"
                f"Current visible text length: {current_chars} characters\n"
                f"Current title: {article.get('title', '')}\n"
                f"Current excerpt: {article.get('excerpt', '')}\n"
                f"Current HTML body: {article.get('content_html', '')}\n"
                "Expansion rules:\n"
                "- Keep the same topic, title intent, SEO intent, and WordPress-friendly HTML.\n"
                "- The final content_html visible text must be at least the minimum length.\n"
                "- Do not pad with filler. Add only useful explanations, examples, comparisons, or cautions.\n"
                "- Add depth to the weakest sections instead of adding more headings.\n"
                "- Do not add FAQ, 함께 읽으면 좋은 글, 관련 글 추천, 마무리, or generic conclusion sections just to increase length.\n"
                "- Use natural Korean that feels written by a knowledgeable person.\n"
                "- Avoid repetitive FAQ/conclusion templates unless they genuinely help.\n"
                "- Keep claims cautious if no source is provided.\n"
                'JSON format: {"title": "...", "slug": "...", "excerpt": "...", '
                '"content_html": "...", "tags": ["..."], "categories": ["..."]}'
            ),
        )

    def compact(self, keyword: str, article: dict[str, Any], min_chars: int, max_chars: int) -> dict[str, Any]:
        """Shorten an article that became too long while keeping reader value."""

        current_chars = visible_text_length(article.get("content_html", ""))
        return self.gpt.json_chat(
            "You are a concise Korean blog editor. Return only valid JSON.",
            (
                "The Korean blog article below is too long for casual readers. Compress it without making it shallow.\n"
                f"Keyword: {keyword}\n"
                f"Target visible Korean text length: {min_chars} to {max_chars} characters\n"
                f"Current visible text length: {current_chars} characters\n"
                f"Current title: {article.get('title', '')}\n"
                f"Current excerpt: {article.get('excerpt', '')}\n"
                f"Current HTML body: {article.get('content_html', '')}\n"
                "Compression rules:\n"
                "- Preserve the answer to the search intent and the most useful examples.\n"
                "- Remove repeated explanations, filler transitions, excessive lists, and boilerplate endings first.\n"
                "- Remove FAQ unless it is clearly necessary for this exact keyword.\n"
                "- Remove sections like 함께 읽으면 좋은 글, 관련 글 추천, 마무리, 결론, 실전 가이드, and generic checklist sections unless essential.\n"
                "- Keep h2/h3 structure, but merge or delete weak sections.\n"
                "- End naturally in 2 to 4 sentences.\n"
                "- Keep the body in WordPress-friendly HTML.\n"
                'JSON format: {"title": "...", "slug": "...", "excerpt": "...", '
                '"content_html": "...", "tags": ["..."], "categories": ["..."]}'
            ),
        )


def visible_text_length(html: str) -> int:
    """Estimate article length by removing HTML tags and whitespace."""

    if not html:
        return 0
    text = re.sub(r"<[^>]+>", " ", html)
    text = re.sub(r"&nbsp;|&amp;|&lt;|&gt;|&quot;|&#39;", " ", text)
    text = re.sub(r"\s+", "", text)
    return len(text)


def ensure_target_length(
    pipeline: ArticlePipeline,
    keyword: str,
    article: dict[str, Any],
    minimum_chars: int = 1600,
    maximum_chars: int = 2200,
    max_expansion_rounds: int = 1,
    max_compaction_rounds: int = 1,
) -> dict[str, Any]:
    """Keep article length readable: not too thin, not too long."""

    for _ in range(max_expansion_rounds):
        if visible_text_length(article.get("content_html", "")) >= minimum_chars:
            break
        article = pipeline.expand(keyword, article, minimum_chars)

    for _ in range(max_compaction_rounds):
        if visible_text_length(article.get("content_html", "")) <= maximum_chars:
            break
        article = pipeline.compact(keyword, article, minimum_chars, maximum_chars)
    return article


class WordPressClient:
    """WordPress REST API client for posts, categories, and tags."""

    def __init__(self, config: Config) -> None:
        self.config = config

    def _auth_header(self) -> dict[str, str]:
        """Build the Basic Auth header using a WordPress app password."""

        token = base64.b64encode(
            f"{self.config.wordpress_username}:{self.config.wordpress_app_password}".encode()
        ).decode()
        return {"Authorization": f"Basic {token}"}

    def create_post(self, article: dict[str, Any]) -> dict[str, Any]:
        """Create a WordPress post and attach categories/tags by ID."""

        require(self.config.wordpress_base_url, "WORDPRESS_BASE_URL")
        require(self.config.wordpress_username, "WORDPRESS_USERNAME")
        require(self.config.wordpress_app_password, "WORDPRESS_APP_PASSWORD")
        validate_article(article)

        category_ids = self._resolve_terms("categories", article.get("categories", []))
        tag_ids = self._resolve_terms("tags", article.get("tags", []))

        response = requests.post(
            f"{self.config.wordpress_base_url}/wp-json/wp/v2/posts",
            headers=self._auth_header(),
            json={
                "title": article.get("title", ""),
                "slug": article.get("slug", ""),
                "excerpt": article.get("excerpt", ""),
                "content": article.get("content_html", ""),
                "status": self.config.wordpress_status,
                "categories": category_ids,
                "tags": tag_ids,
            },
            timeout=30,
        )
        response.raise_for_status()
        return response.json()

    def _resolve_terms(self, endpoint: str, names: Any) -> list[int]:
        """Convert category/tag names into WordPress term IDs."""

        if not isinstance(names, list):
            return []
        ids: list[int] = []
        for raw_name in names[:8]:
            name = str(raw_name).strip()
            if not name:
                continue
            term_id = self._get_or_create_term(endpoint, name)
            if term_id:
                ids.append(term_id)
        return ids

    def _get_or_create_term(self, endpoint: str, name: str) -> int | None:
        """Find an existing term by name, or create it when missing."""

        base = f"{self.config.wordpress_base_url}/wp-json/wp/v2/{endpoint}"
        search = requests.get(
            base,
            headers=self._auth_header(),
            params={"search": name, "per_page": 20},
            timeout=20,
        )
        search.raise_for_status()
        for term in search.json():
            if str(term.get("name", "")).strip().lower() == name.lower():
                return int(term["id"])

        created = requests.post(
            base,
            headers=self._auth_header(),
            json={"name": name},
            timeout=20,
        )
        if created.status_code == 400:
            data = created.json()
            term_id = data.get("data", {}).get("term_id")
            if term_id:
                return int(term_id)
            return None
        created.raise_for_status()
        return int(created.json()["id"])


def validate_article(article: dict[str, Any]) -> None:
    """Ensure GPT returned the fields WordPress needs."""

    required_fields = ["title", "slug", "excerpt", "content_html"]
    missing = [field for field in required_fields if not str(article.get(field, "")).strip()]
    if missing:
        raise RuntimeError(f"Generated article is missing required fields: {', '.join(missing)}")


def generate_preview(keyword: str | None = None) -> dict[str, Any]:
    """Generate one article preview without uploading it to WordPress."""

    config = load_config()
    gpt = GptClient(config)
    keywords = [keyword] if keyword else KeywordProvider(config, gpt).collect()
    if not keywords:
        raise RuntimeError("No keywords collected.")

    pipeline = ArticlePipeline(config, gpt)

    # Preview mode returns the first article candidate as JSON for a UI or tool.
    for keyword in keywords:
        article = normalize_article_for_keyword(pipeline.generate(keyword), keyword)
        verification = pipeline.quick_review(keyword, article)

        # Revision rounds use cheap non-web review. Live web search is reserved for final factual approval.
        for _ in range(max(0, config.max_revision_rounds)):
            score = int(verification.get("score", 0))
            approved = bool(verification.get("approved")) and score >= config.min_verification_score
            if approved:
                break
            article = normalize_article_for_keyword(pipeline.revise(keyword, article, verification), keyword)
            verification = pipeline.quick_review(keyword, article)

        article = normalize_article_for_keyword(
            ensure_target_length(
                pipeline,
                keyword,
                article,
                config.article_min_chars,
                config.article_max_chars,
                config.max_expansion_rounds,
            ),
            keyword,
        )
        article = normalize_article_for_keyword(pipeline.proofread(article), keyword)
        article = normalize_article_for_keyword(
            ensure_target_length(
                pipeline,
                keyword,
                article,
                config.article_min_chars,
                config.article_max_chars,
                0,
            ),
            keyword,
        )

        # Final gate: one live web fact-check on the actual article that will be shown/published.
        verification = pipeline.verify(keyword, article)
        score = int(verification.get("score", 0))
        critical_factual_error = bool(verification.get("critical_factual_error"))
        approved = bool(verification.get("approved")) and score >= config.min_verification_score and not critical_factual_error

        # If the final web check found a serious factual problem, repair once using its sourced notes and re-check once.
        if critical_factual_error and config.max_revision_rounds > 0:
            article = normalize_article_for_keyword(pipeline.revise(keyword, article, verification), keyword)
            article = normalize_article_for_keyword(
                ensure_target_length(
                    pipeline,
                    keyword,
                    article,
                    config.article_min_chars,
                    config.article_max_chars,
                    0,
                ),
                keyword,
            )
            verification = pipeline.verify(keyword, article)
            score = int(verification.get("score", 0))
            critical_factual_error = bool(verification.get("critical_factual_error"))
            approved = bool(verification.get("approved")) and score >= config.min_verification_score and not critical_factual_error

        validate_article(article)
        return {
            "keyword": keyword,
            "article": article,
            "verification": verification,
            "official_images": verification.get("official_images", []),
            "approved": approved,
            "critical_factual_error": critical_factual_error,
            "score": score,
            "usage": gpt.usage_summary(),
        }
    raise RuntimeError("No article generated.")

def collect_keywords_json(category: str | None = None, count: int | None = None) -> dict[str, Any]:
    """Return keyword candidates as JSON for a keyword-management UI."""

    config = load_config()
    requested_count = count or config.keyword_count
    excluded_topics = os.getenv("EXCLUDED_TOPICS", "").strip()

    gpt = GptClient(config)

    if category:
        # Category mode biases keywords toward the selected management tab/category.
        payload = gpt.json_chat(
            "You are a Korean blog keyword researcher. Return only valid JSON.",
            (
                "한국어 블로그용 키워드 후보를 만들어줘.\n"
                f"선택 카테고리: {category}\n"
                f"대상 독자: {config.blog_audience}\n"
                f"필요 개수: {requested_count}\n"
                f"이미 발행했거나 검토 중인 글 제목 목록:\n{excluded_topics or '- 없음'}\n"
                "조건:\n"
                "- 반드시 선택 카테고리와 직접 관련된 키워드만 제안\n"
                "- 위 제목 목록과 검색 의도가 거의 같은 키워드만 제외할 것\n"
                "- 같은 게임명/브랜드명/도구명이 들어가도 세부 의도가 다르면 허용할 것\n"
                "- 사람들이 실제로 검색할 법한 구체적 표현 우선\n"
                "- 너무 넓은 단어보다 실제 검색창에 그대로 입력할 법한 표현 우선\n"
                "- '실전 가이드', '완벽 가이드', '총정리', '한눈에 정리', 'A to Z', '최신 트렌드' 같은 제목형 표현 금지\n"
                'JSON 형식: {"keywords": ["키워드1", "키워드2"]}'
            ),
        )
        keywords = normalize_keywords(payload.get("keywords", []), requested_count)
    elif excluded_topics:
        # Exclusion mode avoids repeating already-published or in-review topics.
        payload = gpt.json_chat(
            "You are a Korean blog keyword researcher. Return only valid JSON.",
            (
                "한국어 블로그용 키워드 후보를 만들어줘.\n"
                f"블로그 주제: {config.blog_niche}\n"
                f"대상 독자: {config.blog_audience}\n"
                f"필요 개수: {requested_count}\n"
                f"이미 발행했거나 검토 중인 글 제목 목록:\n{excluded_topics}\n"
                "조건:\n"
                "- 위 제목 목록과 검색 의도가 거의 같은 키워드만 제외할 것\n"
                "- 제목만 살짝 바꾼 변형 키워드도 제외할 것\n"
                "- 같은 브랜드/도구/게임/제품명이라도 세부 의도가 다르면 허용할 것\n"
                "- 사람들이 실제로 검색할 법한 구체적 표현 우선\n"
                'JSON 형식: {"keywords": ["키워드1", "키워드2"]}'
            ),
        )
        keywords = normalize_keywords(payload.get("keywords", []), requested_count)
    else:
        keywords = KeywordProvider(config, gpt).collect()

    return {"category": category or "", "keywords": keywords}


def trend_json(keyword: str) -> dict[str, Any]:
    """Return Naver DataLab trend points for one keyword."""

    config = load_config()
    require(config.naver_client_id, "NAVER_CLIENT_ID")
    require(config.naver_client_secret, "NAVER_CLIENT_SECRET")
    response = requests.post(
        "https://openapi.naver.com/v1/datalab/search",
        headers={
            "X-Naver-Client-Id": config.naver_client_id,
            "X-Naver-Client-Secret": config.naver_client_secret,
            "Content-Type": "application/json",
        },
        json={
            "startDate": config.naver_start_date,
            "endDate": config.naver_end_date,
            "timeUnit": "date",
            "keywordGroups": [{"groupName": keyword, "keywords": [keyword]}],
        },
        timeout=20,
    )
    response.raise_for_status()
    data = response.json()
    result = data.get("results", [{}])[0]
    return {
        "keyword": keyword,
        "startDate": data.get("startDate", ""),
        "endDate": data.get("endDate", ""),
        "points": result.get("data", []),
    }


def run_batch() -> int:
    """Run the full automation: keyword -> article -> final web verification -> upload."""

    config = load_config()
    gpt = GptClient(config)
    keywords = KeywordProvider(config, gpt).collect()
    if not keywords:
        print("No keywords collected.")
        return 1

    pipeline = ArticlePipeline(config, gpt)
    wordpress = WordPressClient(config)
    uploaded = 0

    for keyword in keywords:
        if uploaded >= config.posts_per_run:
            break

        print(f"\n[Keyword] {keyword}")
        article = normalize_article_for_keyword(pipeline.generate(keyword), keyword)
        verification = pipeline.quick_review(keyword, article)

        # Cheap pre-review catches style/SEO/thin-content problems before the expensive web fact-check.
        for _ in range(max(0, config.max_revision_rounds)):
            score = int(verification.get("score", 0))
            approved = bool(verification.get("approved")) and score >= config.min_verification_score
            if approved:
                break
            article = normalize_article_for_keyword(pipeline.revise(keyword, article, verification), keyword)
            verification = pipeline.quick_review(keyword, article)

        article = normalize_article_for_keyword(
            ensure_target_length(
                pipeline,
                keyword,
                article,
                config.article_min_chars,
                config.article_max_chars,
                config.max_expansion_rounds,
            ),
            keyword,
        )
        article = normalize_article_for_keyword(pipeline.proofread(article), keyword)
        article = normalize_article_for_keyword(
            ensure_target_length(
                pipeline,
                keyword,
                article,
                config.article_min_chars,
                config.article_max_chars,
                0,
            ),
            keyword,
        )

        # Final gate: the only normal live web search pass. This preserves factual accuracy.
        verification = pipeline.verify(keyword, article)
        score = int(verification.get("score", 0))
        critical_factual_error = bool(verification.get("critical_factual_error"))
        approved = bool(verification.get("approved")) and score >= config.min_verification_score and not critical_factual_error

        # Repair and re-check once only when the web gate finds a critical factual problem.
        if critical_factual_error and config.max_revision_rounds > 0:
            article = normalize_article_for_keyword(pipeline.revise(keyword, article, verification), keyword)
            article = normalize_article_for_keyword(
                ensure_target_length(
                    pipeline,
                    keyword,
                    article,
                    config.article_min_chars,
                    config.article_max_chars,
                    0,
                ),
                keyword,
            )
            verification = pipeline.verify(keyword, article)
            score = int(verification.get("score", 0))
            critical_factual_error = bool(verification.get("critical_factual_error"))
            approved = bool(verification.get("approved")) and score >= config.min_verification_score and not critical_factual_error

        print(f"[Verification] approved={approved} critical={critical_factual_error} score={score}")
        if verification.get("issues"):
            print("[Issues]", "; ".join(map(str, verification["issues"])))
        if not approved:
            continue

        validate_article(article)
        if config.dry_run:
            print("[Dry run] WordPress upload skipped.")
            print(json.dumps(article, ensure_ascii=False, indent=2))
        else:
            result = wordpress.create_post(article)
            print(f"[WordPress] created id={result.get('id')} link={result.get('link')}")
        uploaded += 1

    print(f"\nUploaded {uploaded} post(s). Token usage: {gpt.usage_summary()}")
    return 0


if __name__ == "__main__":
    try:
        # CLI modes used by the batch script and any future keyword-management UI.
        if len(sys.argv) >= 2 and sys.argv[1] == "--preview-json":
            requested_keyword = sys.argv[2] if len(sys.argv) >= 3 else None
            print(json.dumps(generate_preview(requested_keyword), ensure_ascii=False))
        elif len(sys.argv) >= 2 and sys.argv[1] == "--keywords-json":
            selected_category = sys.argv[2] if len(sys.argv) >= 3 else None
            selected_count = int(sys.argv[3]) if len(sys.argv) >= 4 else None
            print(json.dumps(collect_keywords_json(selected_category, selected_count), ensure_ascii=False))
        elif len(sys.argv) >= 3 and sys.argv[1] == "--trend-json":
            print(json.dumps(trend_json(sys.argv[2]), ensure_ascii=False))
        else:
            raise SystemExit(run_batch())
    except Exception as exc:
        print(f"Error: {exc}", file=sys.stderr)
        raise SystemExit(1)
