-- AutoBlog MariaDB recovery schema
-- Reconstructed from the Java models and MyBatis mappers in C:\ProjectAll\blog_site\blog_site.
-- Safe to re-run: this script does not DROP databases or tables.

CREATE DATABASE IF NOT EXISTS `blog_site`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `blog_site`;

CREATE TABLE IF NOT EXISTS `wordpress_sites` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(120) NOT NULL,
    `description` VARCHAR(255) NULL,
    `site_url` VARCHAR(500) NOT NULL,
    `posts_api_url` VARCHAR(500) NOT NULL,
    `admin_email` VARCHAR(255) NULL,
    `username` VARCHAR(255) NOT NULL,
    `application_password` VARCHAR(255) NOT NULL,
    `timezone` VARCHAR(80) NOT NULL DEFAULT '(UTC+09:00) 서울',
    `language` VARCHAR(40) NOT NULL DEFAULT '한국어',
    `default_post_status` VARCHAR(30) NOT NULL DEFAULT 'draft',
    `seo_title_suffix` VARCHAR(120) NULL,
    `meta_description_template` VARCHAR(255) NULL,
    `auto_keyword_collection` TINYINT(1) NOT NULL DEFAULT 1,
    `auto_content_generation` TINYINT(1) NOT NULL DEFAULT 1,
    `auto_publishing` TINYINT(1) NOT NULL DEFAULT 0,
    `schedule_enabled` TINYINT(1) NOT NULL DEFAULT 0,
    `schedule_start_time` VARCHAR(5) NULL,
    `scheduled_keyword_count` INT NOT NULL DEFAULT 5,
    `scheduled_post_count` INT NOT NULL DEFAULT 1,
    `schedule_category` VARCHAR(120) NULL,
    `last_scheduled_run_date` DATE NULL,
    `notification_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `notification_email` VARCHAR(255) NULL,
    `notify_on_publish` TINYINT(1) NOT NULL DEFAULT 0,
    `auto_image_generation` TINYINT(1) NOT NULL DEFAULT 1,
    `auto_internal_links` TINYINT(1) NOT NULL DEFAULT 1,
    `active` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_wordpress_sites_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `blog_posts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `site_id` BIGINT NOT NULL,
    `wordpress_post_id` BIGINT NULL,
    `title` VARCHAR(255) NOT NULL,
    `summary` TEXT NOT NULL,
    `content` LONGTEXT NOT NULL,
    `category` VARCHAR(80) NOT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT '임시저장',
    `tags` VARCHAR(255) NULL,
    `cover_image_url` VARCHAR(500) NULL,
    `wordpress_url` VARCHAR(500) NULL,
    `view_count` BIGINT NOT NULL DEFAULT 0,
    `published_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blog_posts_site_wordpress` (`site_id`, `wordpress_post_id`),
    KEY `idx_blog_posts_site_id` (`site_id`),
    KEY `idx_blog_posts_category` (`category`),
    KEY `idx_blog_posts_status` (`status`),
    KEY `idx_blog_posts_published_at` (`published_at`),
    KEY `idx_blog_posts_view_count` (`view_count`),
    CONSTRAINT `fk_blog_posts_site`
        FOREIGN KEY (`site_id`) REFERENCES `wordpress_sites` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `generated_drafts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `site_id` BIGINT NOT NULL,
    `keyword` VARCHAR(255) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `slug` VARCHAR(255) NOT NULL,
    `excerpt` TEXT NOT NULL,
    `content_html` LONGTEXT NOT NULL,
    `tags` VARCHAR(500) NULL,
    `categories` VARCHAR(500) NULL,
    `wordpress_category_id` BIGINT NULL,
    `verification_score` INT NOT NULL DEFAULT 0,
    `approved` TINYINT(1) NOT NULL DEFAULT 0,
    `issues` TEXT NULL,
    `prompt_tokens` INT NOT NULL DEFAULT 0,
    `completion_tokens` INT NOT NULL DEFAULT 0,
    `total_tokens` INT NOT NULL DEFAULT 0,
    `official_images` LONGTEXT NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'review',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_generated_drafts_site_id` (`site_id`),
    KEY `idx_generated_drafts_status` (`status`),
    CONSTRAINT `fk_generated_drafts_site`
        FOREIGN KEY (`site_id`) REFERENCES `wordpress_sites` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `keywords` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `site_id` BIGINT NOT NULL,
    `keyword` VARCHAR(255) NOT NULL,
    `group_name` VARCHAR(120) NOT NULL DEFAULT '자동 수집',
    `status` VARCHAR(30) NOT NULL DEFAULT 'active',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_keywords_site_id` (`site_id`),
    KEY `idx_keywords_status` (`status`),
    CONSTRAINT `fk_keywords_site`
        FOREIGN KEY (`site_id`) REFERENCES `wordpress_sites` (`id`)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Current code reads and writes this column, while the old schema.sql omitted it.
ALTER TABLE `generated_drafts`
    ADD COLUMN IF NOT EXISTS `official_images` LONGTEXT NULL AFTER `total_tokens`;

-- Register a site after replacing the placeholders below.
-- Keep this commented until the real WordPress application password is entered.
/*
INSERT INTO `wordpress_sites` (
    `name`, `description`, `site_url`, `posts_api_url`, `admin_email`,
    `username`, `application_password`, `timezone`, `language`,
    `default_post_status`, `active`
) VALUES (
    '사이트 이름',
    NULL,
    'https://example.com',
    'https://example.com/wp-json/wp/v2/posts',
    'admin@example.com',
    'wordpress_username',
    'wordpress_application_password',
    '(UTC+09:00) 서울',
    '한국어',
    'draft',
    1
);
*/

-- Verification: expected result is four rows, one per application table.
SELECT
    `table_name`,
    `table_rows`,
    `table_collation`
FROM `information_schema`.`tables`
WHERE `table_schema` = 'blog_site'
  AND `table_name` IN ('wordpress_sites', 'blog_posts', 'generated_drafts', 'keywords')
ORDER BY `table_name`;
