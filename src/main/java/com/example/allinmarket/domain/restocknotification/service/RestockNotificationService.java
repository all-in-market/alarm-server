package com.example.allinmarket.domain.restocknotification.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationResponse;
import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.restocknotification.repository.RestockNotificationRepository;
import com.example.allinmarket.domain.restocknotification.sender.RestockNotificationSender;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import com.example.allinmarket.domain.restocksubscription.repository.RestockSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestockNotificationService {

    private final RestockSubscriptionRepository subscriptionRepository;
    private final RestockNotificationRepository notificationRepository;
    private final RestockNotificationSender restockNotificationSender;

    public void notify(Long productId) {
        List<RestockSubscription> subscriptions = subscriptionRepository
                .findAllByProductIdAndStatus(productId, SubscriptionStatusEnum.ACTIVE);

        subscriptions.forEach(subscription ->
                restockNotificationSender.send(subscription, productId)
        );
    }

    // RestockNotificationService.java에 추가
    public List<RestockNotificationResponse> getUnread(Long userId) {
        return notificationRepository.findAllByUserIdAndIsReadFalse(userId)
                .stream()
                .map(RestockNotificationResponse::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        RestockNotification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new BaseException(ErrorEnum.NOTIFICATION_NOT_FOUND));
        notification.read();
    }
}
