package com.back.boundedContext.post.out;

import com.back.boundedContext.market.domain.MarketMember;
import com.back.boundedContext.market.domain.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByBuyer(MarketMember buyer);
}
