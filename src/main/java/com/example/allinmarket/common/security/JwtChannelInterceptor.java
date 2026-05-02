package com.example.allinmarket.common.security;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                throw new BaseException(ErrorEnum.UNAUTHORIZED);
            }

            token = token.substring(7);

            if (!jwtProvider.validateToken(token)) {
                throw new BaseException(ErrorEnum.UNAUTHORIZED);
            }

            Long userId = jwtProvider.getUserId(token);
            log.info("WebSocket principal 설정: {}", userId);
            accessor.setUser(() -> userId.toString());

            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put("userId", userId.toString());
            }

            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        // SUBSCRIBE 등 이후 프레임: session attributes에서 userId 복원
        // StompSubProtocolHandler는 HTTP 핸드셰이크 principal만 전파하므로 직접 설정해야 함
        if (accessor.getCommand() != null) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                String userId = (String) sessionAttributes.get("userId");
                if (userId != null) {
                    accessor.setUser(() -> userId);
                    return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
                }
            }
        }

        return message;
    }
}
