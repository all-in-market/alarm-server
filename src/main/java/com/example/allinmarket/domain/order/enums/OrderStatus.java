package com.example.allinmarket.domain.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    CREATED("CREATED", "주문이 생성되었습니다."),
    PAID("PAID", "결제가 완료되었습니다."),
    SHIPPED("SHIPPED", "배송이 시작되었습니다."),
    DELIVERED("DELIVERED", "배송이 완료되었습니다."),
    REFUNDED("REFUNDED", "환불 완료된 주문입니다."),
    FAILED("FAILED", "실패 처리된 주문입니다.");

    private final String status;
    private final String message;

    public boolean canTransitToTargetStatus(OrderStatus targetStatus) {
        if(targetStatus == null){
            return false;
        }

        return switch (this) {
            case CREATED -> targetStatus == PAID || targetStatus == FAILED;
            case PAID -> targetStatus == SHIPPED || targetStatus == REFUNDED;
            case SHIPPED -> targetStatus == DELIVERED;
            case DELIVERED -> targetStatus == REFUNDED;
            case REFUNDED, FAILED -> false;
        };
    }
}

