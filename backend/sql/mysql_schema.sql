-- 西南大学校园游览 GIS 系统
-- MySQL 8.x 建表脚本

CREATE DATABASE IF NOT EXISTS swu_gis
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE swu_gis;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_user_account_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS guide_location (
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

CREATE TABLE IF NOT EXISTS user_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_user_session_token UNIQUE (token),
  CONSTRAINT fk_user_session_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS visit_log (
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
