package com.example.allinmarket.common.redis;

import com.example.allinmarket.domain.restocknotification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber {

    private final SimpMessagingTemplate messagingTemplate;

    public void onMessage(NotificationEvent event) {
        log.info("Redis event received={}", event);
        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                "/queue/notifications",
                event
        );

        log.debug("Redis 알림 수신 및 websocket 전송 완료");
    }
}