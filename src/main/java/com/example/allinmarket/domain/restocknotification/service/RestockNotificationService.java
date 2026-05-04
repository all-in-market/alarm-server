package com.example.allinmarket.domain.restocknotification.service;

import com.example.allinmarket.domain.restocknotification.sender.RestockNotificationSender;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import com.example.allinmarket.domain.restocksubscription.repository.RestockSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestockNotificationService {

    private final RestockSubscriptionRepository subscriptionRepository;
    private final RestockNotificationSender restockNotificationSender;

    public void notify(Long productId) {
        List<RestockSubscription> subscriptions = subscriptionRepository
                .findAllByProductIdAndStatus(productId, SubscriptionStatusEnum.ACTIVE);

        subscriptions.forEach(subscription ->
                restockNotificationSender.send(subscription, productId)
        );
    }
}
