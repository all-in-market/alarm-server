package com.example.allinmarket.domain.notification.restocknotification.event;

import com.example.allinmarket.domain.notification.restocknotification.entity.RestockNotification;

import java.time.LocalDateTime;

public record RestockNotificationEvent(
        Long userId,
        Long notificationId,
        Long productId,
        String message,
        LocalDateTime createdAt
) {
    public static RestockNotificationEvent from(RestockNotification notification) {
        return new RestockNotificationEvent(
                notification.getUserId(),
                notification.getId(),
                notification.getProductId(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }
}