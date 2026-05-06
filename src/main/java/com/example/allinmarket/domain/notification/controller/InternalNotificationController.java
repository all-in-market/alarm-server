package com.example.allinmarket.domain.notification.controller;

import com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto.OrderStatusUpdateEventRequest;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.service.OrderStatusUpdateNotificationService;
import com.example.allinmarket.domain.notification.restocknotification.dto.RestockEventRequest;
import com.example.allinmarket.domain.notification.restocknotification.service.RestockNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final RestockNotificationService restockNotificationService;
    private final OrderStatusUpdateNotificationService orderStatusUpdateNotificationService;

    @PostMapping("/restock")
    public ResponseEntity<Void> handleRestock(@RequestBody RestockEventRequest request) {
        restockNotificationService.notify(request.productId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/orders")
    public ResponseEntity<Void> handleOrderStatusUpdate(@RequestBody OrderStatusUpdateEventRequest request) {
        orderStatusUpdateNotificationService.notify(request);
        return ResponseEntity.ok().build();
    }
}
