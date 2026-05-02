package com.example.allinmarket.domain.restocknotification.controller;

import com.example.allinmarket.domain.restocknotification.dto.RestockEventRequest;
import com.example.allinmarket.domain.restocknotification.service.RestockNotificationService;
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

    @PostMapping("/restock")
    public ResponseEntity<Void> handleRestock(@RequestBody RestockEventRequest request) {
        restockNotificationService.notify(request.productId());
        return ResponseEntity.ok().build();
    }
}
