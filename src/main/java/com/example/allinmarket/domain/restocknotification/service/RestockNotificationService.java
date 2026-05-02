package com.example.allinmarket.domain.restocknotification.service;

import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationResponse;
import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.restocknotification.repository.RestockNotificationRepository;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import com.example.allinmarket.domain.restocksubscription.repository.RestockSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestockNotificationService {

    private RestockSubscriptionRepository subscriptionRepository;
    private final RestockNotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void notify(Long productId) {
        List<RestockSubscription> subscriptions = subscriptionRepository
                .findAllByProductIdAndStatus(productId, SubscriptionStatusEnum.ACTIVE);

        for(RestockSubscription subscription : subscriptions) {
            try{
                RestockNotification notification = notificationRepository.save(
                        RestockNotification.of(subscription.getUserId(), productId)
                );

                messagingTemplate.convertAndSendToUser(
                        subscription.getUserId().toString(),
                        "/queue/notifications",
                        RestockNotificationResponse.from(notification)
                );

                subscription.send();

            } catch (Exception e) {
                log.error("알림 발송 실패: userId = {}, productId = {}",
                        subscription.getUserId(), productId, e);
            }
        }
    }
}
