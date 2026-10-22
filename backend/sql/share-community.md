# 分享社区数据库说明（在已有库上增量 / 完全重建）

您已执行：

1. `mysql_full_setup.sql`
2. `mysql_update_for_triggers_and_routes.sql`

请再执行：

3. **`mysql_update_share_posts.sql`**

执行后重启 Spring Boot 后端（`GpsApplication`）。

---

## 一、增量更新（推荐，保留现有用户与景点数据）

在 MySQL 客户端或 Navicat 中：

```sql
SOURCE backend/sql/mysql_update_share_posts.sql;
```

或在命令行：

```bash
mysql -u root -p swu_gis < backend/sql/mysql_update_share_posts.sql
```

### 本脚本会创建 / 修改

| 对象 | 说明 |
|------|------|
| `guide_location.image_urls_json` | 多图 JSON（若不存在则添加） |
| `share_post` | 分享帖子（美食 FOOD / 住宿 HOTEL，仅公开） |
| `share_post_comment` | 评论 |
| `share_post_like` | 点赞（同一用户对同一帖唯一） |

### `share_post` 主要字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `user_id` | BIGINT | 发帖用户，外键 `user_account.id` |
| `category` | VARCHAR(20) | `FOOD` 或 `HOTEL` |
| `title` | VARCHAR(120) | 标题 |
| `content` | VARCHAR(2000) | 正文（会同步到 AI 知识库） |
| `image_url` | VARCHAR(500) | 配图 URL |
| `title_as_image` | TINYINT(1) | 是否标题渲染为图 |
| `like_count` / `comment_count` | INT | 计数 |
| `rag_document_id` | BIGINT NULL | 对应 `rag_document` 记录，供 RAG 检索 |

**评论不会写入知识库**；发帖/更新时后端将「标题 + 正文」写入 `rag_document`，分类为 `USER_FOOD` / `USER_HOTEL`。

---

## 二、帖子图片存储（文件系统，非数据库）

后端 `application.yml` 中 `app.media.upload-dir` 默认为 `uploads_media`：

| 分类 | 磁盘目录 | 访问 URL |
|------|----------|----------|
| 美食 | `uploads_media/food_user/{用户ID}/` | `/media/food_user/{userId}/{文件名}` |
| 住宿 | `uploads_media/hotel_user/{用户ID}/` | `/media/hotel_user/{userId}/{文件名}` |

删除帖子时，后端会尝试删除对应图片文件。

---

## 三、完全重建数据库（清空所有数据）

仅在需要「从零开始」时使用。

### 1. 删除库并重建

```sql
DROP DATABASE IF EXISTS swu_gis;
CREATE DATABASE swu_gis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE swu_gis;
```

### 2. 按顺序执行 SQL

```sql
SOURCE .../mysql_full_setup.sql;
SOURCE .../mysql_update_for_triggers_and_routes.sql;
SOURCE .../mysql_update_share_posts.sql;
```

### 3. 删除分享相关表时的顺序（单独清分享数据时）

```sql
USE swu_gis;
DROP TABLE IF EXISTS share_post_like;
DROP TABLE IF EXISTS share_post_comment;
DROP TABLE IF EXISTS share_post;
-- 然后重新执行 mysql_update_share_posts.sql 中建表部分
```

---

## 四、验证

```sql
USE swu_gis;
SHOW TABLES LIKE 'share_%';
DESC share_post;
SELECT category, COUNT(*) FROM share_post GROUP BY category;
```

启动后端后，Android 模拟器登录 → **分享** 发帖 → 在管理端或数据库查看 `share_post` 与 `rag_document` 是否新增 `USER_FOOD` / `USER_HOTEL` 文档。

---

## 五、Android 功能对应

| 功能 | 入口 |
|------|------|
| 美食/住宿分享列表 | 地图底栏「分享」或顶部「美食」「住宿」 |
| 发帖 | 分享页右下角 + |
| 我的分享 | 「我的」→「我的分享」 |
| 点赞/评论 | 帖子详情 |
| 途经数据库景点（蓝色可点） | 路线卡片列表，点击进入景点详情 |
