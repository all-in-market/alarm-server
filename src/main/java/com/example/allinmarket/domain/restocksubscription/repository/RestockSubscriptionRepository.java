package com.example.allinmarket.domain.restocksubscription.repository;

import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestockSubscriptionRepository extends JpaRepository<RestockSubscription, Long> {

    void deleteByUserIdAndProductId(Long userId, Long productId);

    Optional<RestockSubscription> findByUserIdAndProductId(Long buyerId, Long productId);

    List<RestockSubscription> findAllByProductIdAndStatus(Long productId, SubscriptionStatusEnum status);
}
