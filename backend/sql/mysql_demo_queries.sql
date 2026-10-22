-- 典型 GIS 业务 SQL（可用于答辩展示）
USE swu_gis;

-- 1) 查询所有讲解点
SELECT id, name, category, address, latitude, longitude, radius_meters
FROM guide_location
ORDER BY id;

-- 2) 查询某个范围内的讲解点（简化版：经纬度矩形范围）
-- 以西南大学中心区域为例
SELECT id, name, category, latitude, longitude
FROM guide_location
WHERE latitude BETWEEN 29.8190 AND 29.8285
  AND longitude BETWEEN 106.4170 AND 106.4238
ORDER BY id;

-- 3) 查询热门讲解点（播放次数 Top 5）
SELECT l.id,
       l.name,
       COUNT(v.id) AS play_count
FROM guide_location l
LEFT JOIN visit_log v ON v.location_id = l.id
GROUP BY l.id, l.name
ORDER BY play_count DESC
LIMIT 5;

-- 4) 查询今日自动讲解与手动讲解次数
SELECT trigger_type, COUNT(*) AS cnt
FROM visit_log
WHERE created_at >= CURDATE()
GROUP BY trigger_type;
