package com.example.gps.geofence;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gps.network.dto.CheckInDto;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.network.dto.TriggeredLocationDto;
import com.example.gps.util.DistanceUtil;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 地理围栏与自动讲解：距离判断、防重复播报、签到触发结果处理。
 */
public class GeoFenceManager {

    public interface AutoPlayListener {
        void onAutoPlay(@NonNull LocationDto location, @Nullable Double distanceMeters);
    }

    /** 本地围栏命中结果（按距离升序）。 */
    public static final class LocalFenceHit {
        @NonNull
        public final LocationDto location;
        public final double distanceMeters;

        public LocalFenceHit(@NonNull LocationDto location, double distanceMeters) {
            this.location = location;
            this.distanceMeters = distanceMeters;
        }
    }

    public interface FenceTargetResolver {
        @Nullable
        GeoPoint resolveTarget(@NonNull LocationDto location);
    }

    private final Map<Long, Long> autoPlayHistory = new HashMap<>();
    private final Set<Long> playedInSession = new HashSet<>();
    private final long defaultCooldownMs;

    public GeoFenceManager(long defaultCooldownMs) {
        this.defaultCooldownMs = defaultCooldownMs;
    }

    public static long resolveCooldownMs(@NonNull LocationDto location, long defaultCooldownMs) {
        if (location.cooldownSeconds > 0) {
            return location.cooldownSeconds * 1000L;
        }
        return defaultCooldownMs;
    }

    public boolean isInsideFence(double distanceMeters, int radiusMeters) {
        return DistanceUtil.isWithinRadius(distanceMeters, radiusMeters);
    }

    public double distanceMeters(@NonNull GeoPoint user, @NonNull GeoPoint target) {
        return DistanceUtil.distanceMeters(user, target);
    }

    public boolean canAutoPlay(@NonNull LocationDto location) {
        long cooldownMs = resolveCooldownMs(location, defaultCooldownMs);
        long now = System.currentTimeMillis();
        long last = autoPlayHistory.getOrDefault(location.id, 0L);
        return now - last >= cooldownMs;
    }

    public void markPlayed(long locationId) {
        long now = System.currentTimeMillis();
        autoPlayHistory.put(locationId, now);
        playedInSession.add(locationId);
    }

    /** 用户离开围栏时调用，允许再次进入时触发（仍受冷却时间约束）。 */
    public void markLeftFence(long locationId) {
        playedInSession.remove(locationId);
    }

    public void syncActiveFences(@Nullable List<TriggeredLocationDto> triggered) {
        if (triggered == null || triggered.isEmpty()) {
            syncActiveFenceIds(new HashSet<>());
            return;
        }
        Set<Long> inside = new HashSet<>();
        for (TriggeredLocationDto item : triggered) {
            if (item != null) {
                inside.add(item.locationId);
            }
        }
        syncActiveFenceIds(inside);
    }

    /** 根据本地判定结果同步「仍在围栏内/已离开」状态。 */
    public void syncActiveFenceIds(@NonNull Set<Long> insideLocationIds) {
        if (insideLocationIds.isEmpty()) {
            for (Long id : new HashSet<>(playedInSession)) {
                markLeftFence(id);
            }
            return;
        }
        for (Long id : new HashSet<>(playedInSession)) {
            if (!insideLocationIds.contains(id)) {
                markLeftFence(id);
            }
        }
    }

    /**
     * 客户端实时围栏判定：distance(user, target) &lt; radius。
     * 不依赖网络，用于 GPS 更新时即时触发自动讲解。
     */
    @NonNull
    public List<LocalFenceHit> evaluateLocalHits(@NonNull GeoPoint user,
                                                 @NonNull List<LocationDto> locations,
                                                 int defaultRadiusMeters,
                                                 @NonNull FenceTargetResolver resolver) {
        List<LocalFenceHit> hits = new ArrayList<>();
        for (LocationDto location : locations) {
            if (location == null) {
                continue;
            }
            GeoPoint target = resolver.resolveTarget(location);
            if (target == null) {
                continue;
            }
            int radius = location.radiusMeters > 0 ? location.radiusMeters : defaultRadiusMeters;
            double distance = distanceMeters(user, target);
            if (isInsideFence(distance, radius)) {
                hits.add(new LocalFenceHit(location, distance));
            }
        }
        hits.sort(Comparator.comparingDouble(hit -> hit.distanceMeters));
        return hits;
    }

    public void resetSessionPlayed() {
        playedInSession.clear();
    }

    /**
     * 处理后端签到结果，在允许时触发自动讲解。
     *
     * @return 是否已触发自动讲解
     */
    public boolean handleCheckInResult(@NonNull CheckInDto data,
                                       boolean blockAutoPlay,
                                       @NonNull AutoPlayListener listener) {
        if (!data.shouldAutoPlay || data.recommendedLocation == null || blockAutoPlay) {
            return false;
        }
        LocationDto location = data.recommendedLocation;
        if (!canAutoPlay(location)) {
            return false;
        }
        Double distance = findDistance(data.triggeredLocations, location.id);
        markPlayed(location.id);
        listener.onAutoPlay(location, distance);
        return true;
    }

    @Nullable
    public static Double findDistance(@Nullable List<TriggeredLocationDto> triggered, long id) {
        if (triggered == null) {
            return null;
        }
        for (TriggeredLocationDto item : triggered) {
            if (item.locationId == id) {
                return item.distanceMeters;
            }
        }
        return null;
    }
}
