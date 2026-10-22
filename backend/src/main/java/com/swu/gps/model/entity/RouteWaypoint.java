package com.swu.gps.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "route_waypoint")
public class RouteWaypoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private UserRoute route;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "location_id")
    private GuideLocation location;

    @Column(nullable = false)
    private Integer waypointOrder;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column
    private Integer stayDurationMinutes = 15;

    @Column(length = 500)
    private String notes;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserRoute getRoute() {
        return route;
    }

    public void setRoute(UserRoute route) {
        this.route = route;
    }

    public GuideLocation getLocation() {
        return location;
    }

    public void setLocation(GuideLocation location) {
        this.location = location;
    }

    public Integer getWaypointOrder() {
        return waypointOrder;
    }

    public void setWaypointOrder(Integer waypointOrder) {
        this.waypointOrder = waypointOrder;
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

    public Integer getStayDurationMinutes() {
        return stayDurationMinutes;
    }

    public void setStayDurationMinutes(Integer stayDurationMinutes) {
        this.stayDurationMinutes = stayDurationMinutes;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
