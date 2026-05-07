package com.example.allinmarket.common.security;

import com.example.allinmarket.domain.notification.restocknotification.service.RestockNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "notification.auth.client-id=inventory-service",
        "notification.auth.secret=test-secret-key"
})
class InternalAuthFilterIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    RestockNotificationService restockNotificationService;

    private static final String CLIENT_ID = "inventory-service";
    private static final String SECRET    = "test-secret-key";
    private static final String ENDPOINT  = "/internal/notifications/restock";
    private static final String BODY      = "{\"productId\":1}";

    @BeforeEach
    void cleanRedis() {
        Set<String> keys = stringRedisTemplate.keys("internal:req:*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private MockHttpServletRequestBuilder signedPost(String timestamp, String requestId, String body) {
        String sig = HmacSigner.sign(SECRET, timestamp + requestId + body);
        return MockMvcRequestBuilders.post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-Id",  CLIENT_ID)
                .header("X-Timestamp",  timestamp)
                .header("X-Request-Id", requestId)
                .header("X-Signature",  sig)
                .content(body);
    }

    private MockHttpServletRequestBuilder signedPostWithSecret(
            String timestamp, String requestId, String body, String secret) {
        String sig = HmacSigner.sign(secret, timestamp + requestId + body);
        return MockMvcRequestBuilders.post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-Id",  CLIENT_ID)
                .header("X-Timestamp",  timestamp)
                .header("X-Request-Id", requestId)
                .header("X-Signature",  sig)
                .content(body);
    }

    // ── test cases ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("정상 서명된 요청은 200을 반환한다")
    void validSignedRequest_returns200() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestId = UUID.randomUUID().toString();

        mockMvc.perform(signedPost(timestamp, requestId, BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 서명은 401을 반환한다")
    void invalidSignature_returns401() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestId = UUID.randomUUID().toString();

        mockMvc.perform(signedPostWithSecret(timestamp, requestId, BODY, "wrong-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("만료된 타임스탬프(10분 전) 요청은 401을 반환한다")
    void expiredTimestamp_returns401() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond() - 600);
        String requestId = UUID.randomUUID().toString();

        // 서명은 올바르게 생성하여 타임스탬프 검증에서 먼저 차단됨을 확인
        mockMvc.perform(signedPost(timestamp, requestId, BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("동일한 requestId 재사용 시 두 번째 요청은 401을 반환한다")
    void replayAttack_secondRequest_returns401() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestId = UUID.randomUUID().toString();

        mockMvc.perform(signedPost(timestamp, requestId, BODY))
                .andExpect(status().isOk());

        mockMvc.perform(signedPost(timestamp, requestId, BODY))
                .andExpect(status().isUnauthorized());
    }
}
