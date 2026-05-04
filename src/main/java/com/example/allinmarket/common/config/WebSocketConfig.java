package com.example.allinmarket.common.config;

import com.example.allinmarket.common.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // topic -> broadcast용, queue -> 개인 알림용
        registry.enableSimpleBroker("/queue", "/topic");
        // websocket 통해 client action 받을 거면 필요(알림 읽음 처리, ping, typing 등). 일단 두는 게 표준
        registry.setApplicationDestinationPrefixes("/app");
        // "/user/queue/notifications" 활성화 (중요)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(
                                message,
                                StompHeaderAccessor.class
                        );
                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Principal user = accessor.getUser();
                    log.info("SUBSCRIBE 수신: destination={}, user={}, sessionId = {}",
                            accessor.getDestination(),
                            user != null ? user.getName() : null,
                            accessor.getSessionId());
                    log.info(
                            "SUBSCRIBE headers={}",
                            accessor.toNativeHeaderMap()
                    );
                    log.info(
                            "SUBSCRIBE simpUser={}",
                            accessor.getHeader("simpUser")
                    );
                }
                return message;
            }
        });
    }
}
