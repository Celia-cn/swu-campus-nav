-- =============================================
-- 分享社区（美食/住宿帖子、评论、点赞）+ 扩展字段
-- 在已执行 mysql_full_setup.sql 与 mysql_update_for_triggers_and_routes.sql 之后运行
-- 若需完全重建库，请使用 mysql_full_setup_v2.sql（若提供）
-- =============================================

USE swu_gis;

-- guide_location 多图 JSON（若尚未添加）
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'guide_location' AND COLUMN_NAME = 'image_urls_json'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE guide_location ADD COLUMN image_urls_json TEXT NULL AFTER image_url',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================
-- 分享帖子（仅公开）
-- =============================================
CREATE TABLE IF NOT EXISTS share_post (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL COMMENT 'FOOD 或 HOTEL',
  title VARCHAR(120) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  image_url VARCHAR(500) NOT NULL COMMENT '配图 URL，可为上传图或标题转图',
  title_as_image TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1=标题已渲染为图片',
  like_count INT NOT NULL DEFAULT 0,
  comment_count INT NOT NULL DEFAULT 0,
  rag_document_id BIGINT NULL COMMENT '同步到 AI 知识库的文档 ID',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_share_post_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE,
  INDEX idx_share_post_category_created (category, created_at DESC),
  INDEX idx_share_post_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS share_post_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(1000) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_share_comment_post FOREIGN KEY (post_id) REFERENCES share_post(id) ON DELETE CASCADE,
  CONSTRAINT fk_share_comment_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE,
  INDEX idx_share_comment_post (post_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS share_post_like (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  post_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_share_post_like UNIQUE (post_id, user_id),
  CONSTRAINT fk_share_like_post FOREIGN KEY (post_id) REFERENCES share_post(id) ON DELETE CASCADE,
  CONSTRAINT fk_share_like_user FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 说明：
-- 1) 帖子图片目录（后端）：uploads_media/food_user/{用户ID}/ 与 uploads_media/hotel_user/{用户ID}/
-- 2) 访问 URL：/media/food_user/{userId}/{文件名} 、/media/hotel_user/{userId}/{文件名}
-- 3) 帖子正文会写入 rag_document（category=USER_FOOD / USER_HOTEL），评论不写入知识库
-- 4) 重建库时 DROP 顺序：share_post_like, share_post_comment, share_post, 再 drop 其它表
