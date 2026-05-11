package com.example.allinmarket.domain.notification.orderstatusupdatenotification.repository;

import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderStatusUpdateNotificationRepository extends JpaRepository<OrderStatusUpdateNotification, Long> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderStatusUpdateNotification o SET o.isRead = true WHERE o.userId = :buyerId AND o.isRead = false")
    void markAllAsReadByUserId(@Param("buyerId") Long buyerId);

    @Query("SELECT o from OrderStatusUpdateNotification o WHERE o.userId = :buyerId AND o.isRead = false")
    Page<OrderStatusUpdateNotification> findByUserId(@Param("buyerId") Long buyerId, Pageable pageable);

    @Query("SELECT o FROM OrderStatusUpdateNotification o WHERE o.userId = :buyerId AND o.orderId = :orderId")
    Optional<OrderStatusUpdateNotification> findByUserIdAndOrderId(@Param("buyerId") Long buyerId, @Param("orderId") Long orderId);
}
