package com.example.allinmarket.domain.restocknotification.repository;

import com.example.allinmarket.domain.restocknotification.entity.RestockNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestockNotificationRepository extends JpaRepository<RestockNotification, Long> {

    List<RestockNotification> findAllByUserIdAndIsReadFalse(Long userId);
    Optional<RestockNotification> findByIdAndUserId(Long id, Long userId);
}
