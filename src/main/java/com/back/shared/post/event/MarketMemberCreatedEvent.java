package com.back.shared.post.event;

import com.back.shared.post.dto.MarketMemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketMemberCreatedEvent {
    private final MarketMemberDto member;
}