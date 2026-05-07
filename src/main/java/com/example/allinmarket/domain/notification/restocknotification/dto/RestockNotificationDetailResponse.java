package com.example.allinmarket.domain.notification.restocknotification.dto;

import com.example.allinmarket.domain.notification.restocknotification.entity.RestockNotification;

public record RestockNotificationDetailResponse(
        Long buyerId,
        Long productId
) {
    public static RestockNotificationDetailResponse of(RestockNotification restockNotification) {
        return new RestockNotificationDetailResponse(
                restockNotification.getUserId(),
                restockNotification.getProductId()
        );
    }
}
