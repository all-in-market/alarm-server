package com.example.allinmarket.buyer.restocknotification.controller;

import com.example.allinmarket.buyer.restocknotification.service.BuyerRestockNotificationService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.notification.restocknotification.dto.RestockNotificationDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/restock-notifications")
public class RestockNotificationController {

    private final BuyerRestockNotificationService buyerRestockNotificationService;

    // 안읽은 알림 전체 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PageResponse<RestockNotificationDetailResponse>>> getNotifications(Pageable pageable) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        PageResponse<RestockNotificationDetailResponse> result = buyerRestockNotificationService.getNotifications(buyerId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.READ_SUCCESS, result)
        );
    }

    // 전체 읽음 처리
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Void>> readAllNotifications() {
        Long buyerId = SecurityUtils.getCurrentUserId();
        buyerRestockNotificationService.readAllNotifications(buyerId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }

    // 개별 읽음 처리
    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> readNotification(@PathVariable Long productId) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        buyerRestockNotificationService.readNotification(buyerId, productId);

        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null)
        );
    }
}
