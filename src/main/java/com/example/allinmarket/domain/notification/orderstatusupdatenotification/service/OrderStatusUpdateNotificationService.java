package com.example.allinmarket.domain.notification.orderstatusupdatenotification.service;

import com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto.OrderStatusUpdateEventRequest;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.sender.OrderStatusUpdateNotificationSender;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusUpdateNotificationService {

    private final OrderRepository orderRepository;
    private final OrderStatusUpdateNotificationSender orderStatusUpdateNotificationSender;

    @Transactional
    public void notify(OrderStatusUpdateEventRequest request) {
        orderStatusUpdateNotificationSender.send(request.buyerId(), request.orderId(), request.status());
    }
}
