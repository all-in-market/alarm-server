package com.example.allinmarket.buyer.orderstatusupdatenotification.controller;

import com.example.allinmarket.buyer.orderstatusupdatenotification.service.BuyerOrderStatusUpdateNotificationService;
import com.example.allinmarket.common.config.GlobalExceptionHandler;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.notification.orderstatusupdatenotification.dto.OrderNotificationResponse;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NULL;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BuyerOrderStatusUpdateNotificationController.class)
@ExtendWith(RestDocumentationExtension.class)
@TestPropertySource(properties = {
        "notification.auth.client-id=test-client",
        "notification.auth.secret=test-secret"
})
class BuyerOrderStatusUpdateNotificationControllerTest {

    @Autowired
    private BuyerOrderStatusUpdateNotificationController controller;

    private MockMvc mockMvc;

    @MockitoBean
    private BuyerOrderStatusUpdateNotificationService service;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        // standaloneSetup: 보안 필터 체인 없이 컨트롤러만 격리해서 테스트
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .apply(documentationConfiguration(restDocumentation))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        // SecurityUtils.getCurrentUserId()가 1L을 반환하도록 설정
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /order-notifications/me - 알림 목록 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("읽지 않은 알림 목록을 페이지로 반환한다")
        void getNotifications_success() throws Exception {
            // given
            OrderNotificationResponse notification = new OrderNotificationResponse(
                    1L, 100L, OrderStatus.PAID, OrderStatus.PAID.getMessage(),
                    LocalDateTime.of(2024, 1, 1, 12, 0),
                    LocalDateTime.of(2024, 1, 1, 12, 0)
            );
            PageResponse<OrderNotificationResponse> pageResponse = new PageResponse<>(
                    List.of(notification), 1, 1, 1L, 10, true
            );
            given(service.getNotifications(eq(1L), any(Pageable.class))).willReturn(pageResponse);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get("/order-notifications/me")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value(SuccessEnum.READ_SUCCESS.getMessage()))
                    .andExpect(jsonPath("$.data.content[0].buyerId").value(1))
                    .andExpect(jsonPath("$.data.content[0].orderId").value(100))
                    .andExpect(jsonPath("$.data.content[0].status").value(OrderStatus.PAID.getStatus()))
                    .andExpect(jsonPath("$.data.content[0].message").value(OrderStatus.PAID.getMessage()))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.currentPage").value(1))
                    .andDo(document("order-notifications/get-list",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            queryParameters(
                                    parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                    parameterWithName("size").description("페이지당 항목 수").optional()
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data.content[].buyerId").type(NUMBER).description("구매자 ID"),
                                    fieldWithPath("data.content[].orderId").type(NUMBER).description("주문 ID"),
                                    fieldWithPath("data.content[].status").type(STRING).description("주문 상태"),
                                    fieldWithPath("data.content[].message").type(STRING).description("알림 메시지"),
                                    fieldWithPath("data.content[].createdAt").type(STRING).description("알림 생성 시간"),
                                    fieldWithPath("data.content[].updatedAt").type(STRING).description("알림 수정 시간"),
                                    fieldWithPath("data.currentPage").type(NUMBER).description("현재 페이지 번호 (1부터 시작)"),
                                    fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 알림 수"),
                                    fieldWithPath("data.size").type(NUMBER).description("페이지당 항목 수"),
                                    fieldWithPath("data.isLast").type(BOOLEAN).description("마지막 페이지 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("알림이 없으면 빈 목록을 반환한다")
        void getNotifications_empty() throws Exception {
            // given
            PageResponse<OrderNotificationResponse> emptyPage = new PageResponse<>(
                    List.of(), 1, 0, 0L, 10, true
            );
            given(service.getNotifications(eq(1L), any(Pageable.class))).willReturn(emptyPage);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get("/order-notifications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andDo(document("order-notifications/get-list-empty",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data.content").type(ARRAY).description("알림 목록 (빈 배열)"),
                                    fieldWithPath("data.currentPage").type(NUMBER).description("현재 페이지 번호"),
                                    fieldWithPath("data.totalPages").type(NUMBER).description("전체 페이지 수"),
                                    fieldWithPath("data.totalElements").type(NUMBER).description("전체 알림 수"),
                                    fieldWithPath("data.size").type(NUMBER).description("페이지당 항목 수"),
                                    fieldWithPath("data.isLast").type(BOOLEAN).description("마지막 페이지 여부")
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 구매자이면 404를 반환한다")
        void getNotifications_buyerNotFound() throws Exception {
            // given
            given(service.getNotifications(eq(1L), any(Pageable.class)))
                    .willThrow(new BaseException(ErrorEnum.BUYER_NOT_FOUND));

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get("/order-notifications/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(ErrorEnum.BUYER_NOT_FOUND.getStatus()))
                    .andExpect(jsonPath("$.message").value(ErrorEnum.BUYER_NOT_FOUND.getMessage()))
                    .andDo(document("order-notifications/get-list-buyer-not-found",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("오류 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data").type(NULL).description("데이터 없음").optional()
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PUT /order-notifications/me - 전체 알림 읽음 처리")
    class ReadAllNotificationsTest {

        @Test
        @DisplayName("모든 알림을 읽음 처리하고 200을 반환한다")
        void readAllNotifications_success() throws Exception {
            // given
            willDoNothing().given(service).readAllNotifications(1L);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.put("/order-notifications/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                    .andDo(document("order-notifications/read-all",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data").type(NULL).description("데이터 없음").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 구매자이면 404를 반환한다")
        void readAllNotifications_buyerNotFound() throws Exception {
            // given
            willThrow(new BaseException(ErrorEnum.BUYER_NOT_FOUND))
                    .given(service).readAllNotifications(1L);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.put("/order-notifications/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(ErrorEnum.BUYER_NOT_FOUND.getStatus()))
                    .andExpect(jsonPath("$.message").value(ErrorEnum.BUYER_NOT_FOUND.getMessage()))
                    .andDo(document("order-notifications/read-all-buyer-not-found",
                            preprocessResponse(prettyPrint()),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("오류 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data").type(NULL).description("데이터 없음").optional()
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("PUT /order-notifications/{orderId} - 단건 알림 읽음 처리")
    class ReadNotificationTest {

        @Test
        @DisplayName("특정 주문의 알림을 읽음 처리하고 200을 반환한다")
        void readNotification_success() throws Exception {
            // given
            willDoNothing().given(service).readNotification(1L, 100L);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.put("/order-notifications/{orderId}", 100L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value(SuccessEnum.UPDATE_SUCCESS.getMessage()))
                    .andDo(document("order-notifications/read-one",
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    parameterWithName("orderId").description("읽음 처리할 주문 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("응답 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data").type(NULL).description("데이터 없음").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("존재하지 않는 구매자이면 404를 반환한다")
        void readNotification_buyerNotFound() throws Exception {
            // given
            willThrow(new BaseException(ErrorEnum.BUYER_NOT_FOUND))
                    .given(service).readNotification(1L, 100L);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.put("/order-notifications/{orderId}", 100L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(ErrorEnum.BUYER_NOT_FOUND.getStatus()))
                    .andExpect(jsonPath("$.message").value(ErrorEnum.BUYER_NOT_FOUND.getMessage()))
                    .andDo(document("order-notifications/read-one-buyer-not-found",
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    parameterWithName("orderId").description("읽음 처리할 주문 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("오류 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data").type(NULL).description("데이터 없음").optional()
                            )
                    ));
        }

        @Test
        @DisplayName("알림이 존재하지 않으면 404를 반환한다")
        void readNotification_notificationNotFound() throws Exception {
            // given
            willThrow(new BaseException(ErrorEnum.NOTIFICATION_NOT_FOUND))
                    .given(service).readNotification(1L, 999L);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.put("/order-notifications/{orderId}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(ErrorEnum.NOTIFICATION_NOT_FOUND.getStatus()))
                    .andExpect(jsonPath("$.message").value(ErrorEnum.NOTIFICATION_NOT_FOUND.getMessage()))
                    .andDo(document("order-notifications/read-one-notification-not-found",
                            preprocessResponse(prettyPrint()),
                            pathParameters(
                                    parameterWithName("orderId").description("읽음 처리할 주문 ID")
                            ),
                            responseFields(
                                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                                    fieldWithPath("status").type(NUMBER).description("HTTP 상태 코드"),
                                    fieldWithPath("message").type(STRING).description("오류 메시지"),
                                    fieldWithPath("timestamp").type(STRING).description("응답 시간"),
                                    fieldWithPath("data").type(NULL).description("데이터 없음").optional()
                            )
                    ));
        }
    }
}
