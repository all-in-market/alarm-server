package com.example.allinmarket.buyer.orderstatusupdatenotification.controller;

import com.example.allinmarket.buyer.orderstatusupdatenotification.service.BuyerOrderStatusUpdateNotificationService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto.OrderNotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/order-notifications")
public class BuyerOrderStatusUpdateNotificationController {

    private final BuyerOrderStatusUpdateNotificationService service;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<OrderNotificationResponse>>> getNotifications(Pageable pageable) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        PageResponse<OrderNotificationResponse> result = service.getNotifications(buyerId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.READ_SUCCESS, result)
        );
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> readAllNotifications() {
        Long buyerId = SecurityUtils.getCurrentUserId();
        service.readAllNotifications(buyerId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> readNotification(@PathVariable Long orderId) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        service.readNotification(buyerId, orderId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }
}
