package com.akash.order.service;

import com.akash.order.dto.OrderEvent;
import com.akash.order.enity.OrderTable;
import com.akash.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReverseOrder {
    @Autowired
    private OrderRepository orderRepository;

    @KafkaListener(topics = "revered-orders", groupId = "orders-group")
    public void reverseOrder(String event) {
        System.out.println("Reverse order event :: " + event);

        try {
            OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);

            Optional<OrderTable> order = orderRepository.findById(orderEvent.getCustomerOrder().getOrderId());
            order.ifPresent(o -> {
                o.setStatus("Failed");
                orderRepository.save(o);
            });
        } catch (Exception e) {
            System.out.println("Exception occured while reverting order details: " + event);
        }
    }
}
