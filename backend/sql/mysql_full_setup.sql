-- =============================================
-- 西南大学校园游览 GIS 系统一键初始化脚本
-- 适配后端项目：D:/SpringBoot/gps
-- =============================================

CREATE DATABASE IF NOT EXISTS swu_gis
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE swu_gis;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS visit_log;
DROP TABLE IF EXISTS user_session;
DROP TABLE IF EXISTS guide_location;
DROP TABLE IF EXISTS user_account;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_user_account_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE guide_location (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(40) NOT NULL,
  address VARCHAR(255) NULL,
  latitude DOUBLE NOT NULL,
  longitude DOUBLE NOT NULL,
  radius_meters INT NOT NULL,
  description VARCHAR(2000) NOT NULL,
  audio_url VARCHAR(500) NULL,
  image_url VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_user_session_token UNIQUE (token),
  CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE visit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  location_id BIGINT NOT NULL,
  trigger_type VARCHAR(20) NOT NULL,
  user_latitude DOUBLE NOT NULL,
  user_longitude DOUBLE NOT NULL,
  distance_meters DOUBLE NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_visit_log_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_visit_log_location FOREIGN KEY (location_id) REFERENCES guide_location(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_visit_log_created_at ON visit_log(created_at);
CREATE INDEX idx_visit_log_trigger_type ON visit_log(trigger_type);
CREATE INDEX idx_visit_log_location_id ON visit_log(location_id);
CREATE INDEX idx_guide_location_name ON guide_location(name);

INSERT INTO guide_location
  (name, category, address, latitude, longitude, radius_meters, description, audio_url, image_url, created_at, updated_at)
VALUES
  ('西南大学中心图书馆', '图书馆', '重庆市北碚区天生路2号 西南大学中心图书馆', 29.8230614, 106.4204318, 70,
   '这里是西南大学中心图书馆，拥有丰富的纸质与电子资源，是同学们学习与科研的重要空间。', '/media/audio/library.mp3', '/media/image/library.jpg', NOW(6), NOW(6)),
  ('25教学楼', '教学楼', '西南大学北碚校区 25教学楼', 29.8251130, 106.4202302, 60,
   '25教学楼是校园重要教学区域，常用于专业课程授课和学术交流活动。', '/media/audio/building25.mp3', '/media/image/building25.jpg', NOW(6), NOW(6)),
  ('地理科学学院', '学院', '西南大学地理科学学院', 29.8241302, 106.4189186, 60,
   '地理科学学院承担地理学相关教学与科研任务，是本项目 GIS 集成研发的重要参考点位。', '/media/audio/geography.mp3', '/media/image/geography.jpg', NOW(6), NOW(6)),
  ('计算机与信息科学学院学术报告厅', '学院', '西南大学计算机与信息科学学院', 29.8244300, 106.4184100, 55,
   '计算机与信息科学学院学术报告厅是举办讲座、学术论坛和技术分享的重要场所。', '/media/audio/cs_hall.mp3', '/media/image/cs_hall.jpg', NOW(6), NOW(6)),
  ('光华楼', '行政楼', '西南大学光华楼', 29.8245400, 106.4179620, 65,
   '光华楼位于校内核心区域，周边教学与服务设施集中，是校园出行的重要参照点。', '/media/audio/guanghua.mp3', '/media/image/guanghua.jpg', NOW(6), NOW(6)),
  ('第三运动场', '运动场', '西南大学第三运动场', 29.8262060, 106.4185040, 80,
   '第三运动场是开展体育教学和课外锻炼的重要场地，日常人流活跃。', '/media/audio/stadium3.mp3', '/media/image/stadium3.jpg', NOW(6), NOW(6)),
  ('研究生院', '学院', '西南大学研究生院', 29.8208890, 106.4176230, 65,
   '研究生院负责研究生培养与管理工作，是研究生学术与事务服务中心。', '/media/audio/graduateschool.mp3', '/media/image/graduateschool.jpg', NOW(6), NOW(6)),
  ('科学教育研究中心', '科研机构', '西南大学科学教育研究中心', 29.8210110, 106.4206650, 60,
   '科学教育研究中心聚焦教育科学研究与实践创新，常举办专题研究活动。', '/media/audio/science_center.mp3', '/media/image/science_center.jpg', NOW(6), NOW(6)),
  ('西南大学三号门', '校门', '西南大学三号门（北碚校区）', 29.8219182, 106.4225865, 90,
   '三号门是校园重要出入口，连接周边城市道路与校内主干道。', '/media/audio/gate3.mp3', '/media/image/gate3.jpg', NOW(6), NOW(6)),
  ('西南大学南门', '校门', '西南大学南门（北碚校区）', 29.8206700, 106.4236000, 100,
   '南门是游客与师生常用出入口之一，周边交通便利。', '/media/audio/south_gate.mp3', '/media/image/south_gate.jpg', NOW(6), NOW(6)),
  ('第二运动场', '运动场', '西南大学第二运动场', 29.8269500, 106.4220100, 80,
   '第二运动场提供多类体育活动空间，是校园健康生活的重要区域。', '/media/audio/stadium2.mp3', '/media/image/stadium2.jpg', NOW(6), NOW(6)),
  ('行政服务中心', '服务中心', '西南大学行政服务中心', 29.8226150, 106.4182630, 60,
   '行政服务中心承担校园事务办理和综合服务功能，便于师生日常办事。', '/media/audio/admin_center.mp3', '/media/image/admin_center.jpg', NOW(6), NOW(6));

-- 说明：
-- 1) 用户账号不在 SQL 中写死，避免 BCrypt 密码哈希不一致。
-- 2) 首次启动后端时会自动创建默认账号：
--    admin / admin123456
--    student01 / 123456
