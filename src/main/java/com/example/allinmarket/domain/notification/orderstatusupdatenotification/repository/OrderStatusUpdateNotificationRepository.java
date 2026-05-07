package com.example.allinmarket.domain.notification.orderstatusupdatenotification.repository;

import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusUpdateNotificationRepository extends JpaRepository<OrderStatusUpdateNotification, Long> {
}
