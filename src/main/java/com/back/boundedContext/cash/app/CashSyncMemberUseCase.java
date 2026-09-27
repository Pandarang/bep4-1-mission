package com.back.boundedContext.cash.app;


import com.back.boundedContext.cash.domain.CashMember;
import com.back.boundedContext.cash.out.CashMemberRepository;
import com.back.global.eventPublisher.EventPublisher;
import com.back.shared.post.dto.CashMemberDto;
import com.back.shared.post.dto.MemberDto;
import com.back.shared.post.event.CashMemberCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CashSyncMemberUseCase {

    private final CashMemberRepository cashMemberRepository;
    private final EventPublisher eventPublisher;

    public CashMember syncMember(MemberDto member) {

        boolean isNew = !cashMemberRepository.existsById(member.getId());

        CashMember _member = cashMemberRepository.save(
                new CashMember(
                        member.getId(),
                        member.getCreateDate(),
                        member.getModifyDate(),
                        member.getUsername(),
                        "",
                        member.getNickname(),
                        member.getActivityScore()
                )
        );

        if (isNew) {

            eventPublisher.publish(
                    new CashMemberCreatedEvent(new CashMemberDto(_member))
            );
        }

        return _member;
    }
}