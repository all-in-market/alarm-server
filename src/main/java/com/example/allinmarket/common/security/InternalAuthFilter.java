package com.example.allinmarket.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalAuthFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Value("${notification.auth.secret}")
    private String secret;

    @Value("${notification.auth.client-id}")
    private String expectedClientId;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String clientId = request.getHeader("X-Client-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String requestId = request.getHeader("X-Request-Id");
        String signature = request.getHeader("X-Signature");

        if (clientId == null || timestamp == null ||
                requestId == null || signature == null) {
            log.warn("Internal auth failed. reason=MISSING_HEADERS path={}", request.getRequestURI());
            unauthorized(response, "Missing authentication headers");
            return;
        }

        if (!expectedClientId.equals(clientId)) {
            log.warn(
                    "Internal auth failed. reason=UNKNOWN_CLIENT clientId={}",
                    clientId
            );
            unauthorized(response, "Unknown client");
            return;
        }

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request, 4096);

        String body = StreamUtils.copyToString(
                wrappedRequest.getInputStream(),
                StandardCharsets.UTF_8
        );

        long now = Instant.now().getEpochSecond();
        long requestTime;
        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn(
                    "Internal auth failed. reason=INVALID_TIMESTAMP clientId={} timestamp={}",
                    clientId,
                    timestamp
            );
            unauthorized(response, "Invalid timestamp");
            return;
        }

        if (Math.abs(now - requestTime) > 300) {
            log.warn(
                    "Internal auth failed. reason=EXPIRED_REQUEST clientId={} requestId={}",
                    clientId,
                    requestId
            );
            unauthorized(response, "Expired request");
            return;
        }

        if (secret == null) {
            throw new IllegalStateException("Internal auth secret not configured");
        }

        String payload = timestamp + requestId + body;
        String expectedSignature = HmacSigner.sign(secret, payload);

        boolean valid = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn(
                    "Internal auth failed. reason=INVALID_SIGNATURE clientId={} requestId={}",
                    clientId,
                    requestId
            );
            unauthorized(response, "Invalid signature");
            return;
        }

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(
                        "internal:req:" + requestId,
                        "1",
                        Duration.ofMinutes(5)
                );
        if (Boolean.FALSE.equals(success)) {
            log.warn(
                    "Internal auth failed. reason=REPLAY_DETECTED clientId={} requestId={}",
                    clientId,
                    requestId
            );
            unauthorized(response, "Replay detected");
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        clientId,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_INTERNAL_CLIENT")
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(wrappedRequest, response);

    }

    private void unauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
