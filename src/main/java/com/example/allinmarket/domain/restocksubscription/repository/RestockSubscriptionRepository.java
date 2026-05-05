package com.example.allinmarket.domain.restocksubscription.repository;

import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface RestockSubscriptionRepository extends JpaRepository<RestockSubscription, Long> {

    void deleteByUserIdAndProductId(Long userId, Long productId);

    Optional<RestockSubscription> findByUserIdAndProductId(Long buyerId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RestockSubscription> findAllByProductIdAndStatus(Long productId, SubscriptionStatusEnum status);
}
