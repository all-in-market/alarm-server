package com.example.allinmarket.buyer.orderstatusupdatenotification.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.repository.BuyerRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto.OrderNotificationResponse;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.repository.OrderStatusUpdateNotificationRepository;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.never;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BuyerOrderStatusUpdateNotificationServiceTest {

    @Mock
    private OrderStatusUpdateNotificationRepository orderStatusUpdateNotificationRepository;

    @Mock
    private BuyerRepository buyerRepository;

    @InjectMocks
    private BuyerOrderStatusUpdateNotificationService service;

    @Nested
    @DisplayName("알림 목록 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("존재하는 구매자의 읽지 않은 알림 목록을 페이지로 반환한다")
        void getNotifications_success() {
            // given
            Long buyerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Buyer buyer = mock(Buyer.class);
            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));

            OrderStatusUpdateNotification notification = OrderStatusUpdateNotification.of(buyerId, 100L, OrderStatus.PAID);
            Page<OrderStatusUpdateNotification> page = new PageImpl<>(List.of(notification), pageable, 1);
            given(orderStatusUpdateNotificationRepository.findByUserId(buyerId, pageable)).willReturn(page);

            // when
            PageResponse<OrderNotificationResponse> result = service.getNotifications(buyerId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content().get(0).buyerId()).isEqualTo(buyerId);
            assertThat(result.content().get(0).orderId()).isEqualTo(100L);
            assertThat(result.content().get(0).status()).isEqualTo(OrderStatus.PAID);

            verify(buyerRepository).findByIdAndDeletedAtIsNull(buyerId);
            verify(orderStatusUpdateNotificationRepository).findByUserId(buyerId, pageable);
        }

        @Test
        @DisplayName("알림이 없으면 빈 페이지를 반환한다")
        void getNotifications_emptyResult() {
            // given
            Long buyerId = 1L;
            Pageable pageable = PageRequest.of(0, 10);

            Buyer buyer = mock(Buyer.class);
            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
            given(orderStatusUpdateNotificationRepository.findByUserId(buyerId, pageable))
                    .willReturn(Page.empty(pageable));

            // when
            PageResponse<OrderNotificationResponse> result = service.getNotifications(buyerId, pageable);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("존재하지 않는 구매자이면 BUYER_NOT_FOUND 예외를 던진다")
        void getNotifications_buyerNotFound() {
            // given
            Long buyerId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getNotifications(buyerId, pageable))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                            .isEqualTo(ErrorEnum.BUYER_NOT_FOUND));

            verify(orderStatusUpdateNotificationRepository, never()).findByUserId(buyerId, pageable);
        }
    }

    @Nested
    @DisplayName("단건 알림 읽음 처리")
    class ReadNotificationTest {

        @Test
        @DisplayName("알림을 읽음 상태로 변경한다")
        void readNotification_success() {
            // given
            Long buyerId = 1L;
            Long orderId = 100L;

            Buyer buyer = mock(Buyer.class);
            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));

            OrderStatusUpdateNotification notification = OrderStatusUpdateNotification.of(buyerId, orderId, OrderStatus.SHIPPED);
            given(orderStatusUpdateNotificationRepository.findByUserIdAndOrderId(buyerId, orderId))
                    .willReturn(Optional.of(notification));

            // when
            service.readNotification(buyerId, orderId);

            // then
            assertThat(notification.isRead()).isTrue();
            verify(orderStatusUpdateNotificationRepository).findByUserIdAndOrderId(buyerId, orderId);
        }

        @Test
        @DisplayName("존재하지 않는 구매자이면 BUYER_NOT_FOUND 예외를 던진다")
        void readNotification_buyerNotFound() {
            // given
            Long buyerId = 999L;
            Long orderId = 100L;

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.readNotification(buyerId, orderId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                            .isEqualTo(ErrorEnum.BUYER_NOT_FOUND));

            verify(orderStatusUpdateNotificationRepository, never()).findByUserIdAndOrderId(buyerId, orderId);
        }

        @Test
        @DisplayName("알림이 존재하지 않으면 NOTIFICATION_NOT_FOUND 예외를 던진다")
        void readNotification_notificationNotFound() {
            // given
            Long buyerId = 1L;
            Long orderId = 999L;

            Buyer buyer = mock(Buyer.class);
            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));
            given(orderStatusUpdateNotificationRepository.findByUserIdAndOrderId(buyerId, orderId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.readNotification(buyerId, orderId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                            .isEqualTo(ErrorEnum.NOTIFICATION_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("전체 알림 읽음 처리")
    class ReadAllNotificationsTest {

        @Test
        @DisplayName("구매자의 모든 알림을 읽음 처리한다")
        void readAllNotifications_success() {
            // given
            Long buyerId = 1L;

            Buyer buyer = mock(Buyer.class);
            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.of(buyer));

            // when
            service.readAllNotifications(buyerId);

            // then
            verify(orderStatusUpdateNotificationRepository).markAllAsReadByUserId(buyerId);
        }

        @Test
        @DisplayName("존재하지 않는 구매자이면 BUYER_NOT_FOUND 예외를 던진다")
        void readAllNotifications_buyerNotFound() {
            // given
            Long buyerId = 999L;

            given(buyerRepository.findByIdAndDeletedAtIsNull(buyerId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.readAllNotifications(buyerId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorEnum())
                            .isEqualTo(ErrorEnum.BUYER_NOT_FOUND));

            verify(orderStatusUpdateNotificationRepository, never()).markAllAsReadByUserId(buyerId);
        }
    }
}
