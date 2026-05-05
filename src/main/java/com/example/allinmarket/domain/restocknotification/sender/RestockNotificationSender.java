package com.example.allinmarket.domain.restocknotification.sender;

import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.restocknotification.event.NotificationEvent;
import com.example.allinmarket.domain.restocknotification.repository.RestockNotificationRepository;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestockNotificationSender {

    private final RestockNotificationRepository notificationRepository;
    private final RedisPublisher redisPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(RestockSubscription subscription, Long productId) {
        RestockNotification notification = notificationRepository.save(
                RestockNotification.of(subscription.getUserId(), productId)
        );

        NotificationEvent event = NotificationEvent.from(notification);

        redisPublisher.publish(event);

        subscription.send();
    }
}