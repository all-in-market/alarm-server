package com.example.allinmarket.domain.notification.orderstatusupdatenotification.sender;

import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.event.OrderStatusUpdateEvent;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.repository.OrderStatusUpdateNotificationRepository;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderStatusUpdateNotificationSender {

    private final OrderStatusUpdateNotificationRepository orderStatusUpdateNotificationRepository;
    private final RedisPublisher redisPublisher;

    @Transactional
    public void send(Long userId, Long orderId, OrderStatus status) {
        OrderStatusUpdateNotification notification = orderStatusUpdateNotificationRepository.save(
                OrderStatusUpdateNotification.of(userId, orderId, status)
        );
        OrderStatusUpdateEvent event = OrderStatusUpdateEvent.from(notification);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        redisPublisher.publishOrderStatusUpdateEvent(event);
                    }
                }
        );
    }

}
