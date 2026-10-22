package com.swu.gps.model.entity;

import com.swu.gps.util.ImageUrlsJsonUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guide_location")
public class GuideLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer radiusMeters;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(length = 500)
    private String audioUrl;

    @Column(length = 500)
    private String imageUrl;

    /** JSON 数组，存储多张配图 URL，与 imageUrl（首张封面）同步 */
    @Column(name = "image_urls_json", columnDefinition = "TEXT")
    private String imageUrlsJson;

    @Column(nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public void setRadiusMeters(Integer radiusMeters) {
        this.radiusMeters = radiusMeters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getImageUrlsJson() {
        return imageUrlsJson;
    }

    public void setImageUrlsJson(String imageUrlsJson) {
        this.imageUrlsJson = imageUrlsJson;
    }

    public List<String> getImageUrls() {
        return ImageUrlsJsonUtil.mergeLegacy(
                ImageUrlsJsonUtil.parse(imageUrlsJson),
                imageUrl);
    }

    public void setImageUrls(List<String> urls) {
        List<String> cleaned = urls == null ? new ArrayList<>() : new ArrayList<>(urls);
        this.imageUrlsJson = ImageUrlsJsonUtil.serialize(cleaned);
        this.imageUrl = cleaned.isEmpty() ? null : cleaned.get(0);
    }

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }



    // 触发模式配置
    @Column(name = "trigger_mode", length = 20)
    private String triggerMode = "GEOFENCE"; // GEOFENCE, QR_CODE, NFC, BLUETOOTH

    // 最小停留时间（秒），避免快速经过时误触发
    @Column(name = "min_stay_seconds")
    private Integer minStaySeconds = 10;

    // 每个用户最大自动触发次数（null表示无限制）
    @Column(name = "max_trigger_count")
    private Integer maxTriggerCount = null;

    // 触发冷却时间（秒），避免重复触发
    @Column(name = "cooldown_seconds")
    private Integer cooldownSeconds = 120;

    // 触发优先级（数字越大优先级越高）
    @Column(name = "trigger_priority")
    private Integer triggerPriority = 0;

    // Getter和Setter方法
    public String getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }

    public Integer getMinStaySeconds() {
        return minStaySeconds;
    }

    public void setMinStaySeconds(Integer minStaySeconds) {
        this.minStaySeconds = minStaySeconds;
    }

    public Integer getMaxTriggerCount() {
        return maxTriggerCount;
    }

    public void setMaxTriggerCount(Integer maxTriggerCount) {
        this.maxTriggerCount = maxTriggerCount;
    }

    public Integer getCooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(Integer cooldownSeconds) {
        this.cooldownSeconds = cooldownSeconds;
    }

    public Integer getTriggerPriority() {
        return triggerPriority;
    }

    public void setTriggerPriority(Integer triggerPriority) {
        this.triggerPriority = triggerPriority;
    }
}
