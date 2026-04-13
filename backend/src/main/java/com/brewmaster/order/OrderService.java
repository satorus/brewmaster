package com.brewmaster.order;

import org.springframework.stereotype.Service;

// TODO: implement in Order feature milestone
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
