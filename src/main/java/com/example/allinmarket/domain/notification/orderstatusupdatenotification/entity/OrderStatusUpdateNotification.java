package com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "order_status_update_notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStatusUpdateNotification extends ModifiableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long orderId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String message;
    private boolean isRead;

    public static OrderStatusUpdateNotification of(Long userId, Long orderId, OrderStatus status) {
        OrderStatusUpdateNotification o = new OrderStatusUpdateNotification();
        o.userId = userId;
        o.orderId = orderId;
        o.status = status;
        o.message = status.getMessage();
        o.isRead = false;

        return o;
    }

    public void read() {
        this.isRead = true;
    }

}
