package com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto;

import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import com.example.allinmarket.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderNotificationResponse(
        Long buyerId,
        Long orderId,
        OrderStatus status,
        String message,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OrderNotificationResponse from(OrderStatusUpdateNotification notification) {
        return new OrderNotificationResponse(
                notification.getUserId(),
                notification.getOrderId(),
                notification.getStatus(),
                notification.getMessage(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
