package com.example.allinmarket.common.redis;

import com.example.allinmarket.common.redis.enums.RedisChannels;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationResponse;
import com.example.allinmarket.domain.restocknotification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(NotificationEvent event) {
        redisTemplate.convertAndSend(
                RedisChannels.NOTIFICATION,
                event
        );
    }
}