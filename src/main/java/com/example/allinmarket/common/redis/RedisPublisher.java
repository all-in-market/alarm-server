package com.example.allinmarket.common.redis;

import com.example.allinmarket.common.redis.enums.RedisChannels;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.event.OrderStatusUpdateEvent;
import com.example.allinmarket.domain.notification.restocknotification.event.RestockNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishRestockEvent(RestockNotificationEvent event) {
        redisTemplate.convertAndSend(
                RedisChannels.RESTOCK_NOTIFICATION,
                event
        );
    }

    public void publishOrderStatusUpdateEvent(OrderStatusUpdateEvent event) {
        redisTemplate.convertAndSend(
                RedisChannels.ORDER_STATUS_UPDATE_NOTIFICATION,
                event
        );
    }
}