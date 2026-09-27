package com.back.shared.post.event;

import com.back.shared.post.dto.CashMemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CashMemberCreatedEvent {

    private final CashMemberDto member;
}
