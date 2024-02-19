package com.akash.stock.service;

import com.akash.stock.dto.OrderEvent;
import com.akash.stock.entity.WareHouse;
import com.akash.stock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class ReverseStock {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @KafkaListener(topics = "reversed-stock", groupId = "stock-group")
    public void reverseStock(String event) {
        System.out.println("Inside reverse stock for order "+event);

        try {
            OrderEvent deliveryEvent = new ObjectMapper().readValue(event, OrderEvent.class);

            Iterable<WareHouse> inv = stockRepository.findByItem(deliveryEvent.getCustomerOrder().getItem());

            inv.forEach(i -> {
                i.setQuantity(i.getQuantity() + deliveryEvent.getCustomerOrder().getQuantity());
                stockRepository.save(i);
            });

            OrderEvent paymentEvent = new OrderEvent();
            paymentEvent.setCustomerOrder(deliveryEvent.getCustomerOrder());
            paymentEvent.setType("PAYMENT_REVERSED");
            kafkaTemplate.send("reversed-payments", paymentEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
