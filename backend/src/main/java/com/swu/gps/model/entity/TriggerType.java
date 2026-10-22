package com.swu.gps.model.entity;

public enum TriggerType {
    AUTO,           // 地理围栏自动触发
    MANUAL,         // 用户手动触发
    QR_CODE,        // 扫码触发
    SCHEDULED,      // 定时触发
    ROUTE_PASSING   // 路线途经触发
}