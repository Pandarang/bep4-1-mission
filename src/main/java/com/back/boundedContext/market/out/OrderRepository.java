package com.back.boundedContext.market.out;

import com.back.boundedContext.market.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Integer> {
}
