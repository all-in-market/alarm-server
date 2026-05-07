package com.example.allinmarket.domain.notification.restocknotification.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "restock_notifications",
        indexes = {
        @Index(
                name = "idx_restock_notifications_user_created_at",
                columnList = "user_id, created_at"
        )
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RestockNotification extends ModifiableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long productId;
    private String message;
    private boolean isRead;

    public static RestockNotification of(Long userId, Long productId) {
        RestockNotification n = new RestockNotification();
        n.userId = userId;
        n.productId = productId;
        n.message = productId + "번 상품이 재입고되었습니다.";
        n.isRead = false;
        return n;
    }

    public void read() {
        this.isRead = true;
    }
}
