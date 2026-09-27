package com.back.shared.post.event;

import com.back.shared.post.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinedEvent {

    private final MemberDto member;
}
