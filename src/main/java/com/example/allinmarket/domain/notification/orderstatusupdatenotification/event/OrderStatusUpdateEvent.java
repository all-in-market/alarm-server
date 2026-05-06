package com.example.allinmarket.domain.notification.orderstatusupdatenotification.event;

import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import com.example.allinmarket.domain.order.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusUpdateEvent(
    Long notificationId,
    Long userId,
    Long orderId,
    OrderStatus status,
    String message,
    LocalDateTime createdAt
){
    public static OrderStatusUpdateEvent from(OrderStatusUpdateNotification notification) {
        return new OrderStatusUpdateEvent(
                notification.getId(),
                notification.getUserId(),
                notification.getOrderId(),
                notification.getStatus(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }
}
