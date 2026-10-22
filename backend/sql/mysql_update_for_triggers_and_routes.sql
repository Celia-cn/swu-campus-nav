-- =============================================
-- 西南大学校园游览 GIS 系统 - 增量更新脚本
-- 用于添加讲解触发增强和路径规划功能
-- 在已运行 mysql_full_setup.sql 的基础上执行
-- =============================================

USE swu_gis;

-- =============================================
-- 第一部分：为 guide_location 表添加触发配置字段
-- =============================================

-- 检查并添加 trigger_mode 字段
SET @dbname = DATABASE();
SET @tablename = 'guide_location';
SET @columnname = 'trigger_mode';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " VARCHAR(20) DEFAULT 'GEOFENCE' AFTER image_url")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查并添加 min_stay_seconds 字段
SET @columnname = 'min_stay_seconds';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " INT DEFAULT 10 AFTER trigger_mode")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查并添加 max_trigger_count 字段
SET @columnname = 'max_trigger_count';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " INT NULL AFTER min_stay_seconds")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查并添加 cooldown_seconds 字段
SET @columnname = 'cooldown_seconds';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " INT DEFAULT 120 AFTER max_trigger_count")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 检查并添加 trigger_priority 字段
SET @columnname = 'trigger_priority';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  "SELECT 1",
  CONCAT("ALTER TABLE ", @tablename, " ADD COLUMN ", @columnname, " INT DEFAULT 0 AFTER cooldown_seconds")
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- =============================================
-- 第二部分：创建触发历史表
-- =============================================

CREATE TABLE IF NOT EXISTS trigger_history (
                                               id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                               user_id BIGINT NOT NULL,
                                               location_id BIGINT NOT NULL,
                                               trigger_type VARCHAR(20) NOT NULL,
    user_latitude DOUBLE NOT NULL,
    user_longitude DOUBLE NOT NULL,
    distance_meters DOUBLE NOT NULL,
    stay_duration_seconds INT NULL,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_trigger_history_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_trigger_history_location FOREIGN KEY (location_id) REFERENCES guide_location(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建索引（使用存储过程方式避免重复创建错误）
DROP PROCEDURE IF EXISTS create_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE create_index_if_not_exists()
BEGIN
  -- 检查索引是否存在
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trigger_history'
    AND INDEX_NAME = 'idx_trigger_history_user'
  ) THEN
ALTER TABLE trigger_history ADD INDEX idx_trigger_history_user (user_id);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trigger_history'
    AND INDEX_NAME = 'idx_trigger_history_location'
  ) THEN
ALTER TABLE trigger_history ADD INDEX idx_trigger_history_location (location_id);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'trigger_history'
    AND INDEX_NAME = 'idx_trigger_history_created'
  ) THEN
ALTER TABLE trigger_history ADD INDEX idx_trigger_history_created (created_at);
END IF;
END$$
DELIMITER ;

CALL create_index_if_not_exists();
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

-- =============================================
-- 第三部分：创建路径规划相关表
-- =============================================

-- 用户自定义路线表
CREATE TABLE IF NOT EXISTS user_route (
                                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          user_id BIGINT NOT NULL,
                                          name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    total_distance_meters DOUBLE NULL,
    estimated_time_minutes INT NULL,
    is_public BOOLEAN DEFAULT FALSE,
    like_count INT DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_user_route_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 路线途经点表
CREATE TABLE IF NOT EXISTS route_waypoint (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              route_id BIGINT NOT NULL,
                                              location_id BIGINT NULL,
                                              waypoint_order INT NOT NULL,
                                              latitude DOUBLE NOT NULL,
                                              longitude DOUBLE NOT NULL,
                                              stay_duration_minutes INT DEFAULT 15,
                                              notes VARCHAR(500) NULL,
    CONSTRAINT fk_route_waypoint_route FOREIGN KEY (route_id) REFERENCES user_route(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_route_waypoint_location FOREIGN KEY (location_id) REFERENCES guide_location(id)
    ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 校园路线节点表
CREATE TABLE IF NOT EXISTS campus_route_node (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 node_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    node_type VARCHAR(20) NOT NULL,
    accessibility_info VARCHAR(200) NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_campus_node_coords (latitude, longitude)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 校园路线边表
CREATE TABLE IF NOT EXISTS campus_route_edge (
                                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                                 from_node_id VARCHAR(50) NOT NULL,
    to_node_id VARCHAR(50) NOT NULL,
    distance_meters DOUBLE NOT NULL,
    estimated_walk_seconds INT NOT NULL,
    estimated_cycle_seconds INT NULL,
    route_type VARCHAR(20) NOT NULL DEFAULT 'WALKING',
    has_stairs BOOLEAN DEFAULT FALSE,
    has_elevator BOOLEAN DEFAULT FALSE,
    is_covered BOOLEAN DEFAULT FALSE,
    difficulty_level INT DEFAULT 1,
    CONSTRAINT uk_route_edge UNIQUE (from_node_id, to_node_id, route_type),
    CONSTRAINT fk_edge_from FOREIGN KEY (from_node_id) REFERENCES campus_route_node(node_id),
    CONSTRAINT fk_edge_to FOREIGN KEY (to_node_id) REFERENCES campus_route_node(node_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 路线收藏表
CREATE TABLE IF NOT EXISTS route_favorite (
                                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                              user_id BIGINT NOT NULL,
                                              route_id BIGINT NOT NULL,
                                              created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_route_favorite UNIQUE (user_id, route_id),
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES user_account(id)
    ON DELETE CASCADE,
    CONSTRAINT fk_favorite_route FOREIGN KEY (route_id) REFERENCES user_route(id)
    ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 为 route_waypoint 创建索引
DROP PROCEDURE IF EXISTS create_route_indexes;
DELIMITER $$
CREATE PROCEDURE create_route_indexes()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'route_waypoint'
    AND INDEX_NAME = 'idx_route_waypoint_route'
  ) THEN
ALTER TABLE route_waypoint ADD INDEX idx_route_waypoint_route (route_id, waypoint_order);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'campus_route_edge'
    AND INDEX_NAME = 'idx_route_edge_from'
  ) THEN
ALTER TABLE campus_route_edge ADD INDEX idx_route_edge_from (from_node_id);
END IF;

  IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'campus_route_edge'
    AND INDEX_NAME = 'idx_route_edge_to'
  ) THEN
ALTER TABLE campus_route_edge ADD INDEX idx_route_edge_to (to_node_id);
END IF;
END$$
DELIMITER ;

CALL create_route_indexes();
DROP PROCEDURE IF EXISTS create_route_indexes;

-- =============================================
-- 第四部分：插入初始校园路线节点数据
-- =============================================

-- 只在表为空时插入数据
INSERT INTO campus_route_node (node_id, name, latitude, longitude, node_type, created_at)
SELECT * FROM (
                  SELECT 'SWU_GATE_S' as node_id, '南门' as name, 29.8206700 as latitude, 106.4236000 as longitude, 'ENTRANCE' as node_type, NOW(6) as created_at
                  UNION ALL SELECT 'SWU_GATE_HUB', '南门广场', 29.8212000, 106.4229000, 'LANDMARK', NOW(6)
                  UNION ALL SELECT 'SWU_MAIN_E', '主干道东段', 29.8225000, 106.4215000, 'INTERSECTION', NOW(6)
                  UNION ALL SELECT 'SWU_TEACH_25', '25教学楼', 29.8251130, 106.4202302, 'BUILDING', NOW(6)
                  UNION ALL SELECT 'SWU_LIBRARY', '中心图书馆', 29.8230614, 106.4204318, 'BUILDING', NOW(6)
                  UNION ALL SELECT 'SWU_CSIS', '计算机与信息科学学院报告厅', 29.8244300, 106.4184100, 'BUILDING', NOW(6)
                  UNION ALL SELECT 'SWU_SPORT_2', '第二运动场', 29.8269500, 106.4220100, 'LANDMARK', NOW(6)
                  UNION ALL SELECT 'SWU_GEO', '地理科学学院', 29.8241302, 106.4189186, 'BUILDING', NOW(6)
                  UNION ALL SELECT 'SWU_SPORT_3', '第三运动场', 29.8262060, 106.4185040, 'LANDMARK', NOW(6)
                  UNION ALL SELECT 'SWU_GRAD', '研究生院', 29.8208890, 106.4176230, 'BUILDING', NOW(6)
                  UNION ALL SELECT 'SWU_WEST_CONN', '西侧连廊', 29.82190, 106.41830, 'INTERSECTION', NOW(6)
                  UNION ALL SELECT 'SWU_NORTH_CONN', '北侧连接点', 29.82385, 106.42090, 'INTERSECTION', NOW(6)
              ) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM campus_route_node LIMIT 1);

-- =============================================
-- 第五部分：插入初始校园路线边数据
-- =============================================

-- 只在表为空时插入数据
INSERT INTO campus_route_edge (from_node_id, to_node_id, distance_meters, estimated_walk_seconds, estimated_cycle_seconds, route_type)
SELECT * FROM (
                  SELECT 'SWU_GATE_S' as from_id, 'SWU_GATE_HUB' as to_id, 80.0 as dist, 64 as walk_sec, 20 as cycle_sec, 'WALKING' as type
                  UNION ALL SELECT 'SWU_GATE_HUB', 'SWU_MAIN_E', 150.0, 120, 38, 'WALKING'
                  UNION ALL SELECT 'SWU_MAIN_E', 'SWU_GEO', 180.0, 144, 45, 'WALKING'
                  UNION ALL SELECT 'SWU_GEO', 'SWU_TEACH_25', 120.0, 96, 30, 'WALKING'
                  UNION ALL SELECT 'SWU_TEACH_25', 'SWU_LIBRARY', 250.0, 200, 63, 'WALKING'
                  UNION ALL SELECT 'SWU_LIBRARY', 'SWU_CSIS', 200.0, 160, 50, 'WALKING'
                  UNION ALL SELECT 'SWU_CSIS', 'SWU_SPORT_2', 300.0, 240, 75, 'WALKING'
                  UNION ALL SELECT 'SWU_TEACH_25', 'SWU_WEST_CONN', 150.0, 120, 38, 'WALKING'
                  UNION ALL SELECT 'SWU_WEST_CONN', 'SWU_GRAD', 180.0, 144, 45, 'WALKING'
                  UNION ALL SELECT 'SWU_GEO', 'SWU_SPORT_3', 220.0, 176, 55, 'WALKING'
                  UNION ALL SELECT 'SWU_MAIN_E', 'SWU_SPORT_3', 280.0, 224, 70, 'WALKING'
                  UNION ALL SELECT 'SWU_GEO', 'SWU_NORTH_CONN', 100.0, 80, 25, 'WALKING'
                  UNION ALL SELECT 'SWU_NORTH_CONN', 'SWU_LIBRARY', 120.0, 96, 30, 'WALKING'
              ) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM campus_route_edge LIMIT 1);

-- =============================================
-- 完成提示
-- =============================================
SELECT '数据库更新完成！已添加讲解触发配置和路径规划功能所需的表和字段。' AS message;