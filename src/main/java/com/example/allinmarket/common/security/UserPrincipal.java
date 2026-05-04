package com.example.allinmarket.common.security;

import com.example.allinmarket.realtimechat.enums.RealtimeChatSenderType;

import java.security.Principal;

public record UserPrincipal(
        Long userId,
        RealtimeChatSenderType senderType
) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
