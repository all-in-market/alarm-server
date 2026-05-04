package com.example.allinmarket.domain.restocknotification.event;

import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;

import java.time.LocalDateTime;

public record NotificationEvent (
        Long userId,
        Long notificationId,
        Long productId,
        String message,
        LocalDateTime createdAt
) {
    public static NotificationEvent from(RestockNotification notification) {
        return new NotificationEvent(
                notification.getUserId(),
                notification.getId(),
                notification.getProductId(),
                notification.getMessage(),
                LocalDateTime.now()
        );
    }
}