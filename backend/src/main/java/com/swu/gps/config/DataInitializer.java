package com.swu.gps.config;

import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.RagCategory;
import com.swu.gps.model.entity.RagDocument;
import com.swu.gps.model.entity.UserAccount;
import com.swu.gps.model.entity.UserRole;
import com.swu.gps.repository.GuideLocationRepository;
import com.swu.gps.repository.RagDocumentRepository;
import com.swu.gps.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final GuideLocationRepository guideLocationRepository;
    private final RagDocumentRepository ragDocumentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAccountRepository userAccountRepository,
                           GuideLocationRepository guideLocationRepository,
                           RagDocumentRepository ragDocumentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.guideLocationRepository = guideLocationRepository;
        this.ragDocumentRepository = ragDocumentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedLocations();
        patchLocationCoordinates();
        migrateImageUrlsJson();
        clearLegacyDefaultMediaUrls();
        seedRagKnowledge();
    }

    private void seedRagKnowledge() {
        if (ragDocumentRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ragDocumentRepository.saveAll(List.of(
                doc(now, "西南大学校史概览", RagCategory.HISTORY,
                        "西南大学溯源于1906年成立的川东师范学堂，2005年由西南师范大学与西南农业大学合并组建。学校坐落于重庆市北碚区缙云山麓、嘉陵江畔，是教育部直属全国重点大学，国家“双一流”建设高校。校园融合了师范教育与农业学科传统，形成综合性研究型大学格局。"),
                doc(now, "校园近期资讯", RagCategory.NEWS,
                        "西南大学常态化开展校园开放日、文化讲座与社团活动。考试季图书馆延长开放，新生报到季各门岗人流较大。校内通知可通过学院官网、学校官方公众号获取；涉及教务、后勤安排请以学校最新公告为准。"),
                doc(now, "北碚周边美食推荐", RagCategory.FOOD,
                        "西南大学北碚校区周边（天生路、吾悦广场、嘉陵风情街一带）有火锅、小面、烧烤、奶茶等餐饮。学生常去天生路小吃街；聚餐可选吾悦广场商圈。北区校门口公交线路较多，晚间人流大，注意错峰。"),
                doc(now, "校内校车与公交", RagCategory.BUS,
                        "西南大学北碚校区面积较大，校内设有校车/摆渡线路连接主要宿舍区、教学楼与校门（具体站点以校内公示为准）。校外可乘重庆轨道交通6号线北碚站，再换乘公交至西南大学相关门岗；常见门岗包括南门、三号门等，请以校内指示牌为准。"),
                doc(now, "校内前往方式说明", RagCategory.ROUTE,
                        "前往西南大学北碚校区：轨道交通6号线至北碚站，转公交或打车至对应校门。校内前往中心图书馆、25教学楼、地理科学学院、光华楼、第三运动场、研究生院、三号门、南门等地点，可步行或乘坐校内校车；也可在「智畅游」地图中搜索讲解点名称查看位置与简介，结合导航步行前往。")
        ));
    }

    private RagDocument doc(LocalDateTime now, String title, RagCategory category, String content) {
        RagDocument d = new RagDocument();
        d.setTitle(title);
        d.setCategory(category);
        d.setContent(content);
        d.setEnabled(true);
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        return d;
    }

    /**
     * 清除种子数据中的默认配图/录音 URL，交由管理员在 Web 端按景点上传。
     */
    private void clearLegacyDefaultMediaUrls() {
        List<GuideLocation> all = guideLocationRepository.findAll();
        boolean changed = false;
        for (GuideLocation location : all) {
            boolean locChanged = false;
            List<String> urls = new ArrayList<>(location.getImageUrls());
            int before = urls.size();
            urls.removeIf(url -> url != null && url.startsWith("/media/image/"));
            if (urls.size() != before) {
                locChanged = true;
            }
            if (location.getAudioUrl() != null && location.getAudioUrl().startsWith("/media/audio/")) {
                location.setAudioUrl(null);
                locChanged = true;
            }
            if (locChanged) {
                location.setImageUrls(urls);
                changed = true;
            }
        }
        if (changed) {
            guideLocationRepository.saveAll(all);
        }
    }

    private void migrateImageUrlsJson() {
        List<GuideLocation> all = guideLocationRepository.findAll();
        boolean changed = false;
        for (GuideLocation location : all) {
            if ((location.getImageUrlsJson() == null || location.getImageUrlsJson().isBlank())
                    && location.getImageUrl() != null && !location.getImageUrl().isBlank()) {
                location.setImageUrls(List.of(location.getImageUrl().trim()));
                changed = true;
            }
        }
        if (changed) {
            guideLocationRepository.saveAll(all);
        }
    }

    /**
     * 仅为「缺少有效经纬度」的景点填入内置 GCJ-02 参考坐标。
     * 不覆盖管理端已保存的坐标，避免重启后端后部分景点回到旧种子位置。
     */
    private void patchLocationCoordinates() {
        Map<String, double[]> canonical = canonicalGcjCoordinates();
        List<GuideLocation> all = guideLocationRepository.findAll();
        boolean changed = false;
        for (GuideLocation location : all) {
            double[] coords = canonical.get(location.getName());
            if (coords == null || hasPlottableCoordinates(location)) {
                continue;
            }
            location.setLatitude(coords[0]);
            location.setLongitude(coords[1]);
            changed = true;
        }
        if (changed) {
            guideLocationRepository.saveAll(all);
        }
    }

    private static boolean hasPlottableCoordinates(GuideLocation location) {
        if (location == null || location.getLatitude() == null || location.getLongitude() == null) {
            return false;
        }
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        return Math.abs(lat) > 1e-4
                && Math.abs(lng) > 1e-4
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180;
    }

    private Map<String, double[]> canonicalGcjCoordinates() {
        Map<String, double[]> map = new LinkedHashMap<>();
        map.put("西南大学中心图书馆", new double[]{29.8230614, 106.4204318});
        map.put("25教学楼", new double[]{29.8251130, 106.4202302});
        map.put("地理科学学院", new double[]{29.8241302, 106.4189186});
        map.put("计算机与信息科学学院学术报告厅", new double[]{29.8244300, 106.4184100});
        map.put("光华楼", new double[]{29.8245400, 106.4179620});
        map.put("第三运动场", new double[]{29.8262060, 106.4185040});
        map.put("研究生院", new double[]{29.8208890, 106.4176230});
        map.put("科学教育研究中心", new double[]{29.8210110, 106.4206650});
        map.put("西南大学三号门", new double[]{29.8219182, 106.4225865});
        map.put("西南大学南门", new double[]{29.8206700, 106.4236000});
        map.put("第二运动场", new double[]{29.8269500, 106.4220100});
        map.put("行政服务中心", new double[]{29.8226150, 106.4182630});
        return map;
    }

    private void seedUsers() {
        if (userAccountRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123456"));
        admin.setRole(UserRole.ADMIN);
        admin.setCreatedAt(now);

        UserAccount demoUser = new UserAccount();
        demoUser.setUsername("student01");
        demoUser.setPasswordHash(passwordEncoder.encode("123456"));
        demoUser.setRole(UserRole.USER);
        demoUser.setCreatedAt(now);

        userAccountRepository.saveAll(List.of(admin, demoUser));
    }

    private void seedLocations() {
        if (guideLocationRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        guideLocationRepository.saveAll(List.of(
                buildLocation(
                        "西南大学中心图书馆",
                        "图书馆",
                        "重庆市北碚区天生路2号 西南大学中心图书馆",
                        29.8230614, 106.4204318, 70,
                        "这里是西南大学中心图书馆，拥有丰富的纸质与电子资源，是同学们学习与科研的重要空间。",
                        now
                ),
                buildLocation(
                        "25教学楼",
                        "教学楼",
                        "西南大学北碚校区 25教学楼",
                        29.8251130, 106.4202302, 60,
                        "25教学楼是校园重要教学区域，常用于专业课程授课和学术交流活动。",
                        now
                ),
                buildLocation(
                        "地理科学学院",
                        "学院",
                        "西南大学地理科学学院",
                        29.8241302, 106.4189186, 60,
                        "地理科学学院承担地理学相关教学与科研任务，是本项目 GIS 集成研发的重要参考点位。",
                        now
                ),
                buildLocation(
                        "计算机与信息科学学院学术报告厅",
                        "学院",
                        "西南大学计算机与信息科学学院",
                        29.8244300, 106.4184100, 55,
                        "计算机与信息科学学院学术报告厅是举办讲座、学术论坛和技术分享的重要场所。",
                        now
                ),
                buildLocation(
                        "光华楼",
                        "行政楼",
                        "西南大学光华楼",
                        29.8245400, 106.4179620, 65,
                        "光华楼位于校内核心区域，周边教学与服务设施集中，是校园出行的重要参照点。",
                        now
                ),
                buildLocation(
                        "第三运动场",
                        "运动场",
                        "西南大学第三运动场",
                        29.8262060, 106.4185040, 80,
                        "第三运动场是开展体育教学和课外锻炼的重要场地，日常人流活跃。",
                        now
                ),
                buildLocation(
                        "研究生院",
                        "学院",
                        "西南大学研究生院",
                        29.8208890, 106.4176230, 65,
                        "研究生院负责研究生培养与管理工作，是研究生学术与事务服务中心。",
                        now
                ),
                buildLocation(
                        "科学教育研究中心",
                        "科研机构",
                        "西南大学科学教育研究中心",
                        29.8210110, 106.4206650, 60,
                        "科学教育研究中心聚焦教育科学研究与实践创新，常举办专题研究活动。",
                        now
                ),
                buildLocation(
                        "西南大学三号门",
                        "校门",
                        "西南大学三号门（北碚校区）",
                        29.8219182, 106.4225865, 90,
                        "三号门是校园重要出入口，连接周边城市道路与校内主干道。",
                        now
                ),
                buildLocation(
                        "西南大学南门",
                        "校门",
                        "西南大学南门（北碚校区）",
                        29.8206700, 106.4236000, 100,
                        "南门是游客与师生常用出入口之一，周边交通便利。",
                        now
                ),
                buildLocation(
                        "第二运动场",
                        "运动场",
                        "西南大学第二运动场",
                        29.8269500, 106.4220100, 80,
                        "第二运动场提供多类体育活动空间，是校园健康生活的重要区域。",
                        now
                ),
                buildLocation(
                        "行政服务中心",
                        "服务中心",
                        "西南大学行政服务中心",
                        29.8226150, 106.4182630, 60,
                        "行政服务中心承担校园事务办理和综合服务功能，便于师生日常办事。",
                        now
                )
        ));
    }

    private GuideLocation buildLocation(String name,
                                        String category,
                                        String address,
                                        double latitude,
                                        double longitude,
                                        int radiusMeters,
                                        String description,
                                        LocalDateTime now) {
        GuideLocation location = new GuideLocation();
        location.setName(name);
        location.setCategory(category);
        location.setAddress(address);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setRadiusMeters(radiusMeters);
        location.setDescription(description);
        location.setAudioUrl(null);
        location.setImageUrls(List.of());
        location.setCreatedAt(now);
        location.setUpdatedAt(now);
        return location;
    }
}
