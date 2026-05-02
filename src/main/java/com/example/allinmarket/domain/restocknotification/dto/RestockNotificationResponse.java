package com.example.allinmarket.domain.restocknotification.dto;

import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;

import java.time.LocalDateTime;

public record RestockNotificationResponse(
        Long id,
        Long userId,
        Long productId,
        String message,
        boolean isRead,
        LocalDateTime createdAt

        ) {
    public static RestockNotificationResponse from(RestockNotification notification) {
        return new RestockNotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getProductId(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
