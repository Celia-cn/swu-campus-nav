package com.swu.gps.service;

import com.swu.gps.exception.ApiException;
import com.swu.gps.model.entity.*;
import com.swu.gps.model.request.*;
import com.swu.gps.model.response.*;
import com.swu.gps.repository.*;
import com.swu.gps.util.GeoUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoutePlanningService {

    private final UserRouteRepository userRouteRepository;
    private final GuideLocationRepository guideLocationRepository;
    private final UserAccountRepository userAccountRepository;

    public RoutePlanningService(UserRouteRepository userRouteRepository,
                                GuideLocationRepository guideLocationRepository,
                                UserAccountRepository userAccountRepository) {
        this.userRouteRepository = userRouteRepository;
        this.guideLocationRepository = guideLocationRepository;
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * 规划两点之间的路线
     */
    public RoutePlanResponse planRoute(RoutePlanRequest request) {
        // 简化版：直接计算直线距离和预估时间
        double distance = GeoUtils.distanceMeters(
                request.startLatitude(), request.startLongitude(),
                request.endLatitude(), request.endLongitude()
        );

        int estimatedTime = estimateTravelTime(distance, request.routeMode());

        List<RoutePlanResponse.GeoPointDto> routePoints = Arrays.asList(
                new RoutePlanResponse.GeoPointDto(request.startLatitude(), request.startLongitude()),
                new RoutePlanResponse.GeoPointDto(request.endLatitude(), request.endLongitude())
        );

        List<RoutePlanResponse.RouteInstruction> instructions = Arrays.asList(
                new RoutePlanResponse.RouteInstruction(0, request.startLatitude(), request.startLongitude(), "从起点出发"),
                new RoutePlanResponse.RouteInstruction(1, request.endLatitude(), request.endLongitude(), "到达目的地")
        );

        return new RoutePlanResponse(routePoints, distance, estimatedTime, instructions);
    }

    /**
     * 规划多景点路线
     */
    public RoutePlanResponse planMultiStopRoute(MultiStopRouteRequest request) {
        List<Long> locationIds = request.locationIds();
        if (locationIds == null || locationIds.size() < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "至少需要2个景点");
        }

        List<GuideLocation> locations = guideLocationRepository.findAllById(locationIds);
        if (locations.size() != locationIds.size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "部分景点不存在");
        }

        // 优化访问顺序
        List<GuideLocation> optimizedOrder = optimizeVisitOrder(
                locations,
                request.startLatitude() != null ? request.startLatitude() : locations.get(0).getLatitude(),
                request.startLongitude() != null ? request.startLongitude() : locations.get(0).getLongitude()
        );

        // 构建路线
        List<RoutePlanResponse.GeoPointDto> allPoints = new ArrayList<>();
        double totalDistance = 0;
        int totalTime = 0;
        List<RoutePlanResponse.RouteInstruction> allInstructions = new ArrayList<>();

        for (int i = 0; i < optimizedOrder.size(); i++) {
            GuideLocation loc = optimizedOrder.get(i);
            allPoints.add(new RoutePlanResponse.GeoPointDto(loc.getLatitude(), loc.getLongitude()));

            if (i > 0) {
                GuideLocation prev = optimizedOrder.get(i - 1);
                double segmentDist = GeoUtils.distanceMeters(
                        prev.getLatitude(), prev.getLongitude(),
                        loc.getLatitude(), loc.getLongitude()
                );
                totalDistance += segmentDist;
                totalTime += estimateTravelTime(segmentDist, request.routeMode());
            }

            allInstructions.add(new RoutePlanResponse.RouteInstruction(
                    i, loc.getLatitude(), loc.getLongitude(),
                    i == 0 ? "从起点出发" : "参观：" + loc.getName()
            ));
        }

        allInstructions.add(new RoutePlanResponse.RouteInstruction(
                allInstructions.size(),
                optimizedOrder.get(optimizedOrder.size() - 1).getLatitude(),
                optimizedOrder.get(optimizedOrder.size() - 1).getLongitude(),
                "行程结束"
        ));

        return new RoutePlanResponse(allPoints, totalDistance, totalTime, allInstructions);
    }

    /**
     * 保存自定义路线
     */
    @Transactional
    public UserRouteSummary saveRoute(UserAccount user, SaveRouteRequest request) {
        UserRoute route = new UserRoute();
        route.setUser(user);
        route.setName(request.name());
        route.setDescription(request.description());
        route.setIsPublic(request.isPublic() != null ? request.isPublic() : false);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());

        int order = 1;
        for (SaveRouteRequest.WaypointRequest wp : request.waypoints()) {
            RouteWaypoint waypoint = new RouteWaypoint();
            waypoint.setRoute(route);
            waypoint.setWaypointOrder(order++);
            waypoint.setLatitude(wp.latitude());
            waypoint.setLongitude(wp.longitude());
            waypoint.setStayDurationMinutes(wp.stayDurationMinutes() != null ? wp.stayDurationMinutes() : 15);
            waypoint.setNotes(wp.notes());

            if (wp.locationId() != null) {
                GuideLocation location = guideLocationRepository.findById(wp.locationId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "景点不存在"));
                waypoint.setLocation(location);
            }

            route.addWaypoint(waypoint);
        }

        calculateRouteStats(route);
        userRouteRepository.save(route);

        return new UserRouteSummary(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getTotalDistanceMeters(),
                route.getEstimatedTimeMinutes(),
                route.getWaypoints().size(),
                route.getIsPublic(),
                route.getLikeCount(),
                route.getCreatedAt()
        );
    }

    /**
     * 获取用户路线列表
     */
    public List<UserRouteSummary> getUserRoutes(Long userId) {
        // 使用预加载waypoints的查询方法，避免懒加载异常
        return userRouteRepository.findByUserIdWithWaypointsOrderByCreatedAtDesc(userId).stream()
                .map(route -> new UserRouteSummary(
                        route.getId(),
                        route.getName(),
                        route.getDescription(),
                        route.getTotalDistanceMeters(),
                        route.getEstimatedTimeMinutes(),
                        route.getWaypoints().size(),
                        route.getIsPublic(),
                        route.getLikeCount(),
                        route.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取路线详情
     */
    public UserRouteDetail getRouteDetail(Long routeId) {
        UserRoute route = userRouteRepository.findByIdWithWaypoints(routeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "路线不存在"));

        List<UserRouteDetail.WaypointDetail> waypoints = route.getWaypoints().stream()
                .map(wp -> new UserRouteDetail.WaypointDetail(
                        wp.getId(),
                        wp.getWaypointOrder(),
                        wp.getLatitude(),
                        wp.getLongitude(),
                        wp.getLocation() != null ? wp.getLocation().getId() : null,
                        wp.getLocation() != null ? wp.getLocation().getName() : null,
                        wp.getStayDurationMinutes(),
                        wp.getNotes()
                ))
                .collect(Collectors.toList());

        return new UserRouteDetail(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getTotalDistanceMeters(),
                route.getEstimatedTimeMinutes(),
                waypoints,
                route.getIsPublic(),
                route.getLikeCount(),
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }

    /**
     * 删除路线
     */
    @Transactional
    public void deleteRoute(UserAccount user, Long routeId) {
        UserRoute route = userRouteRepository.findById(routeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "路线不存在"));

        if (!route.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权删除此路线");
        }

        userRouteRepository.delete(route);
    }

    // 私有辅助方法

    private int estimateTravelTime(double distanceMeters, String mode) {
        double speedMps = 1.25; // 默认步行速度
        if ("CYCLING".equalsIgnoreCase(mode)) {
            speedMps = 4.0;
        }
        return (int) Math.ceil(distanceMeters / speedMps / 60.0);
    }

    private List<GuideLocation> optimizeVisitOrder(List<GuideLocation> locations,
                                                   double startLat, double startLng) {
        List<GuideLocation> optimized = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        double currentLat = startLat;
        double currentLng = startLng;

        while (optimized.size() < locations.size()) {
            GuideLocation nearest = null;
            double minDist = Double.MAX_VALUE;

            for (GuideLocation loc : locations) {
                if (visited.contains(loc.getId())) continue;

                double dist = GeoUtils.distanceMeters(currentLat, currentLng,
                        loc.getLatitude(), loc.getLongitude());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = loc;
                }
            }

            if (nearest != null) {
                optimized.add(nearest);
                visited.add(nearest.getId());
                currentLat = nearest.getLatitude();
                currentLng = nearest.getLongitude();
            }
        }

        return optimized;
    }

    private void calculateRouteStats(UserRoute route) {
        List<RouteWaypoint> waypoints = route.getWaypoints();
        if (waypoints.size() < 2) {
            route.setTotalDistanceMeters(0.0);
            route.setEstimatedTimeMinutes(0);
            return;
        }

        double totalDistance = 0;
        for (int i = 1; i < waypoints.size(); i++) {
            RouteWaypoint prev = waypoints.get(i - 1);
            RouteWaypoint curr = waypoints.get(i);
            totalDistance += GeoUtils.distanceMeters(
                    prev.getLatitude(), prev.getLongitude(),
                    curr.getLatitude(), curr.getLongitude()
            );
        }

        route.setTotalDistanceMeters(totalDistance);
        route.setEstimatedTimeMinutes((int) Math.ceil(totalDistance / 1.25 / 60.0));
    }
}