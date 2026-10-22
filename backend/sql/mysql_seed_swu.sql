-- 西南大学北碚校区 GIS 点位初始化数据
USE swu_gis;

-- 可选：如果希望纯 SQL 初始化账号，请先准备 BCrypt 密码哈希后再插入。
-- 建议直接启动 SpringBoot，让 DataInitializer 自动创建：
-- admin / admin123456
-- student01 / 123456

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
