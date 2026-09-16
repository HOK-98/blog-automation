CREATE DATABASE IF NOT EXISTS autoblog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE autoblog;

CREATE TABLE IF NOT EXISTS blog_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    wordpress_post_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NOT NULL,
    content LONGTEXT NOT NULL,
    category VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT '임시저장',
    tags VARCHAR(255),
    cover_image_url VARCHAR(500),
    wordpress_url VARCHAR(500),
    view_count BIGINT NOT NULL DEFAULT 0,
    published_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_blog_posts_site_id (site_id),
    UNIQUE KEY uk_blog_posts_site_wordpress (site_id, wordpress_post_id),
    INDEX idx_blog_posts_category (category),
    INDEX idx_blog_posts_status (status),
    INDEX idx_blog_posts_published_at (published_at),
    INDEX idx_blog_posts_view_count (view_count)
);

CREATE TABLE IF NOT EXISTS wordpress_sites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    site_url VARCHAR(500) NOT NULL,
    posts_api_url VARCHAR(500) NOT NULL,
    admin_email VARCHAR(255),
    username VARCHAR(255) NOT NULL,
    application_password VARCHAR(255) NOT NULL,
     timezone VARCHAR(80) NOT NULL DEFAULT '(UTC+09:00) 서울',
     language VARCHAR(40) NOT NULL DEFAULT '한국어',
     default_post_status VARCHAR(30) NOT NULL DEFAULT 'draft',
     seo_title_suffix VARCHAR(120),
     meta_description_template VARCHAR(255),
     auto_keyword_collection TINYINT(1) NOT NULL DEFAULT 1,
     auto_content_generation TINYINT(1) NOT NULL DEFAULT 1,
     auto_publishing TINYINT(1) NOT NULL DEFAULT 0,
     schedule_enabled TINYINT(1) NOT NULL DEFAULT 0,
     schedule_start_time VARCHAR(5),
     scheduled_keyword_count INT NOT NULL DEFAULT 5,
     scheduled_post_count INT NOT NULL DEFAULT 1,
     schedule_category VARCHAR(120),
     last_scheduled_run_date DATE,
     notification_enabled TINYINT(1) NOT NULL DEFAULT 1,
     notification_email VARCHAR(255),
     notify_on_publish TINYINT(1) NOT NULL DEFAULT 0,
     auto_image_generation TINYINT(1) NOT NULL DEFAULT 1,
    auto_internal_links TINYINT(1) NOT NULL DEFAULT 1,
    active TINYINT(1) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS generated_drafts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    excerpt TEXT NOT NULL,
     content_html LONGTEXT NOT NULL,
     tags VARCHAR(500),
     categories VARCHAR(500),
     wordpress_category_id BIGINT,
     verification_score INT NOT NULL DEFAULT 0,
     approved TINYINT(1) NOT NULL DEFAULT 0,
     issues TEXT,
     prompt_tokens INT NOT NULL DEFAULT 0,
     completion_tokens INT NOT NULL DEFAULT 0,
     total_tokens INT NOT NULL DEFAULT 0,
     status VARCHAR(30) NOT NULL DEFAULT 'review',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_generated_drafts_site_id (site_id),
    INDEX idx_generated_drafts_status (status)
);

CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    keyword VARCHAR(255) NOT NULL,
    group_name VARCHAR(120) NOT NULL DEFAULT '자동 수집',
    status VARCHAR(30) NOT NULL DEFAULT 'active',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_keywords_site_id (site_id),
    INDEX idx_keywords_status (status)
);
