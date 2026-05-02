package com.example.allinmarket.domain.restocknotification.controller;

import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.domain.restocknotification.dto.RestockNotificationResponse;
import com.example.allinmarket.domain.restocknotification.service.RestockNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// NotificationController.java
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RestockNotificationService notificationService;

    // 안읽은 알림 가져오기
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<RestockNotificationResponse>>> getUnread() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success(SuccessEnum.READ_SUCCESS,
                        notificationService.getUnread(userId))
        );
    }

    // 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        Long userId = SecurityUtils.getCurrentUserId();
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, null));
    }
}