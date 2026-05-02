package com.example.allinmarket.domain.restocknotification.sender;

import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationResponse;
import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.restocknotification.repository.RestockNotificationRepository;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// RestockNotificationSender.java
@Service
@Slf4j
@RequiredArgsConstructor
public class RestockNotificationSender {

    private final RestockNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void send(RestockSubscription subscription, Long productId) {
        try {
            RestockNotification notification = notificationRepository.save(
                    RestockNotification.of(subscription.getUserId(), productId)
            );

            messagingTemplate.convertAndSendToUser(
                    subscription.getUserId().toString(),
                    "/queue/notifications",
                    RestockNotificationResponse.from(notification)
            );
            log.info("알림 발송 완료: userId={}, destination=/user/{}/queue/notifications",
                    subscription.getUserId(), subscription.getUserId());

            subscription.send();

        } catch (Exception e) {
            log.error("알림 발송 실패: userId = {}, productId = {}",
                    subscription.getUserId(), productId, e);
        }
    }
}