package com.example.allinmarket.domain.notification.controller;

import com.example.allinmarket.common.redis.RedisPublisher;
import com.example.allinmarket.common.security.HmacSigner;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.entity.OrderStatusUpdateNotification;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.repository.OrderStatusUpdateNotificationRepository;
import com.example.allinmarket.domain.notification.restocknotification.entity.RestockNotification;
import com.example.allinmarket.domain.notification.restocknotification.repository.RestockNotificationRepository;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;
import com.example.allinmarket.domain.restocksubscription.repository.RestockSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * InternalNotificationController 비즈니스 통합 테스트.
 *
 * 인증 레이어(서명 검증, 타임스탬프 만료, 리플레이 방지)는 InternalAuthFilterIntegrationTest에서 커버하므로
 * 이 클래스는 "인증 통과 이후"의 DB 영속·구독 상태 전이·Redis 이벤트 발행에 집중한다.
 *
 * 인프라 구성:
 * - H2 인메모리 DB (application-test.yml, create-drop)
 * - Testcontainers Redis (InternalAuthFilter의 리플레이 방지용)
 * - RedisPublisher Mock (pub/sub 발행 횟수 검증, 실제 subscriber 불필요)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "notification.auth.client-id=inventory-service",
        "notification.auth.secret=test-secret-key"
})
class InternalNotificationControllerIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired MockMvc mockMvc;
    @Autowired RestockSubscriptionRepository subscriptionRepository;
    @Autowired RestockNotificationRepository restockNotificationRepository;
    @Autowired OrderStatusUpdateNotificationRepository orderNotificationRepository;
    @Autowired StringRedisTemplate stringRedisTemplate;

    // afterCommit() 훅이 실제로 실행되는지와 발행 횟수를 검증하기 위해 Mock 사용.
    // 실제 Redis pub/sub subscriber를 구성하지 않아도 된다.
    @MockitoBean
    RedisPublisher redisPublisher;

    private static final String CLIENT_ID = "inventory-service";
    private static final String SECRET    = "test-secret-key";

    @BeforeEach
    void setUp() {
        restockNotificationRepository.deleteAll();
        orderNotificationRepository.deleteAll();
        subscriptionRepository.deleteAll();

        // 리플레이 방지 키 초기화 (각 테스트 간 requestId 충돌 방지)
        Set<String> keys = stringRedisTemplate.keys("internal:req:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private MockHttpServletRequestBuilder signedPost(String endpoint, String body) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestId = UUID.randomUUID().toString();
        String sig = HmacSigner.sign(SECRET, timestamp + requestId + body);
        return MockMvcRequestBuilders.post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-Id",  CLIENT_ID)
                .header("X-Timestamp",  timestamp)
                .header("X-Request-Id", requestId)
                .header("X-Signature",  sig)
                .content(body);
    }

    // ── Restock ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /internal/notifications/restock")
    class RestockNotificationTests {

        @Test
        @DisplayName("ACTIVE 구독자 수만큼 알림이 DB에 저장된다")
        void notify_withMultipleActiveSubscriptions_savesOneNotificationPerSubscriber() throws Exception {
            subscriptionRepository.saveAll(List.of(
                    RestockSubscription.of(101L, 1L),
                    RestockSubscription.of(102L, 1L)
            ));

            mockMvc.perform(signedPost("/internal/notifications/restock", "{\"productId\":1}"))
                    .andExpect(status().isOk());

            List<RestockNotification> saved = restockNotificationRepository.findAll();
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(RestockNotification::getUserId)
                    .containsExactlyInAnyOrder(101L, 102L);
        }

        @Test
        @DisplayName("알림 전송 후 구독 상태가 ACTIVE → SENT 로 전이되고 lastNotifiedAt 이 기록된다")
        void notify_changesSubscriptionStatusToSentWithTimestamp() throws Exception {
            subscriptionRepository.save(RestockSubscription.of(101L, 1L));

            mockMvc.perform(signedPost("/internal/notifications/restock", "{\"productId\":1}"))
                    .andExpect(status().isOk());

            RestockSubscription sub = subscriptionRepository.findAll().get(0);
            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatusEnum.SENT);
            assertThat(sub.getLastNotifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("SENT 구독은 처리 대상에서 제외된다")
        void notify_skipsNonActiveSubscriptions() throws Exception {
            RestockSubscription active = RestockSubscription.of(101L, 1L);
            RestockSubscription alreadySent = RestockSubscription.of(102L, 1L);
            alreadySent.send(); // ACTIVE → SENT
            subscriptionRepository.saveAll(List.of(active, alreadySent));

            mockMvc.perform(signedPost("/internal/notifications/restock", "{\"productId\":1}"))
                    .andExpect(status().isOk());

            // ACTIVE 구독(101L)만 알림 생성
            assertThat(restockNotificationRepository.count()).isEqualTo(1);
            assertThat(restockNotificationRepository.findAll().get(0).getUserId()).isEqualTo(101L);
        }

        @Test
        @DisplayName("ACTIVE 구독이 없으면 알림이 생성되지 않고 Redis 이벤트도 발행되지 않는다")
        void notify_withNoActiveSubscriptions_createsNothingAndPublishesNothing() throws Exception {
            mockMvc.perform(signedPost("/internal/notifications/restock", "{\"productId\":99}"))
                    .andExpect(status().isOk());

            assertThat(restockNotificationRepository.count()).isZero();
            verify(redisPublisher, never()).publishRestockEvent(any());
        }

        @Test
        @DisplayName("저장된 알림 엔티티의 각 필드가 올바르다")
        void notify_savedNotificationHasCorrectFields() throws Exception {
            subscriptionRepository.save(RestockSubscription.of(200L, 5L));

            mockMvc.perform(signedPost("/internal/notifications/restock", "{\"productId\":5}"))
                    .andExpect(status().isOk());

            RestockNotification notification = restockNotificationRepository.findAll().get(0);
            assertThat(notification.getUserId()).isEqualTo(200L);
            assertThat(notification.getProductId()).isEqualTo(5L);
            assertThat(notification.getMessage()).isEqualTo("5번 상품이 재입고되었습니다.");
            assertThat(notification.isRead()).isFalse();
            assertThat(notification.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("ACTIVE 구독자 수만큼 Redis 재입고 이벤트가 발행된다")
        void notify_publishesRedisEventForEachActiveSubscription() throws Exception {
            subscriptionRepository.saveAll(List.of(
                    RestockSubscription.of(101L, 1L),
                    RestockSubscription.of(102L, 1L)
            ));

            mockMvc.perform(signedPost("/internal/notifications/restock", "{\"productId\":1}"))
                    .andExpect(status().isOk());

            verify(redisPublisher, times(2)).publishRestockEvent(any());
        }
    }

    // ── Order ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /internal/notifications/orders")
    class OrderStatusUpdateNotificationTests {

        @Test
        @DisplayName("주문 상태 변경 알림이 DB에 올바른 필드로 저장된다")
        void notify_savesOrderNotificationWithCorrectFields() throws Exception {
            String body = "{\"buyerId\":100,\"orderId\":200,\"status\":\"PAID\"}";

            mockMvc.perform(signedPost("/internal/notifications/orders", body))
                    .andExpect(status().isOk());

            List<OrderStatusUpdateNotification> saved = orderNotificationRepository.findAll();
            assertThat(saved).hasSize(1);

            OrderStatusUpdateNotification notification = saved.get(0);
            assertThat(notification.getUserId()).isEqualTo(100L);
            assertThat(notification.getOrderId()).isEqualTo(200L);
            assertThat(notification.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(notification.getMessage()).isEqualTo(OrderStatus.PAID.getMessage());
            assertThat(notification.isRead()).isFalse();
        }

        @Test
        @DisplayName("주문 알림 저장 후 Redis 주문 이벤트가 1회 발행된다")
        void notify_publishesOrderRedisEventOnce() throws Exception {
            String body = "{\"buyerId\":100,\"orderId\":200,\"status\":\"SHIPPED\"}";

            mockMvc.perform(signedPost("/internal/notifications/orders", body))
                    .andExpect(status().isOk());

            verify(redisPublisher, times(1)).publishOrderStatusUpdateEvent(any());
        }

        @Test
        @DisplayName("저장된 알림의 message 가 OrderStatus enum 메시지와 일치한다")
        void notify_messageMatchesOrderStatusEnumMessage() throws Exception {
            String body = "{\"buyerId\":100,\"orderId\":300,\"status\":\"DELIVERED\"}";

            mockMvc.perform(signedPost("/internal/notifications/orders", body))
                    .andExpect(status().isOk());

            OrderStatusUpdateNotification notification = orderNotificationRepository.findAll().get(0);
            assertThat(notification.getMessage()).isEqualTo(OrderStatus.DELIVERED.getMessage());
            assertThat(notification.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("REFUNDED 상태 알림도 정상적으로 저장된다")
        void notify_withRefundedStatus_savesNotification() throws Exception {
            String body = "{\"buyerId\":100,\"orderId\":400,\"status\":\"REFUNDED\"}";

            mockMvc.perform(signedPost("/internal/notifications/orders", body))
                    .andExpect(status().isOk());

            OrderStatusUpdateNotification notification = orderNotificationRepository.findAll().get(0);
            assertThat(notification.getStatus()).isEqualTo(OrderStatus.REFUNDED);
            assertThat(notification.getMessage()).isEqualTo(OrderStatus.REFUNDED.getMessage());
        }
    }
}
