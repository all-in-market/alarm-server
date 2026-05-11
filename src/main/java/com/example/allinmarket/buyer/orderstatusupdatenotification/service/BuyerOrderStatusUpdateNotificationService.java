package com.example.allinmarket.buyer.orderstatusupdatenotification.service;

import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto.OrderNotificationResponse;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.repository.OrderStatusUpdateNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerOrderStatusUpdateNotificationService {

    private final OrderStatusUpdateNotificationRepository orderStatusUpdateNotificationRepository;
    private final BuyerRepository buyerRepository;

    public PageResponse<OrderNotificationResponse> getNotifications(Long buyerId, Pageable pageable) {
        validateBuyer(buyerId);
        return PageResponse.register(
                orderStatusUpdateNotificationRepository.findByUserId(buyerId, pageable)
                        .map(OrderNotificationResponse::from)
        );
    }

    @Transactional
    public void readNotification(Long buyerId, Long orderId) {
        validateBuyer(buyerId);
        OrderStatusUpdateNotification notification = orderStatusUpdateNotificationRepository.findByUserIdAndOrderId(buyerId, orderId)
                .orElseThrow(() -> new BaseException(ErrorEnum.NOTIFICATION_NOT_FOUND));
        notification.read();
    }

    @Transactional
    public void readAllNotifications(Long buyerId) {
        validateBuyer(buyerId);
        orderStatusUpdateNotificationRepository.markAllAsReadByUserId(buyerId);
    }

    private void validateBuyer(Long buyerId) {
        buyerRepository.findByIdAndDeletedAtIsNull(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.BUYER_NOT_FOUND)
        );
    }

}
