package com.akash.delivery.controller;

import com.akash.delivery.dto.CustomerOrder;
import com.akash.delivery.dto.OrderEvent;
import com.akash.delivery.entity.Delivery;
import com.akash.delivery.repository.DeliveryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DeliveryController {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @KafkaListener(topics = "new-stock", groupId = "stock-group")
    public void deliverOrder(String event) throws JsonMappingException, JsonProcessingException {
        System.out.println("Inside ship order for order "+event);

        Delivery shipment = new Delivery();
        OrderEvent inventoryEvent = new ObjectMapper().readValue(event, OrderEvent.class);
        CustomerOrder order = inventoryEvent.getCustomerOrder();

        try {
            if (order.getAddress() == null) {
                throw new Exception("Address not present");
            }

            shipment.setAddress(order.getAddress());
            shipment.setOrderId(order.getOrderId());

            shipment.setStatus("success");

            deliveryRepository.save(shipment);
            System.out.println("all microservices processing done");
        } catch (Exception e) {
            shipment.setOrderId(order.getOrderId());
            shipment.setStatus("failed");
            deliveryRepository.save(shipment);

            System.out.println("order is: " + order);

            OrderEvent reverseEvent = new OrderEvent();
            reverseEvent.setType("STOCK_REVERSED");
            reverseEvent.setCustomerOrder(order);
            kafkaTemplate.send("reversed-stock", reverseEvent);
        }
    }

    @GetMapping("/getDeliveryStatusByOrderId/{orderId}")
    public String addItems(@PathVariable long orderId) {
        try {
            Delivery delivery = deliveryRepository.findByOrderId(orderId);
            if(delivery == null || delivery.getId() == null) {
                return "pending";
            }

            if(delivery != null && delivery.getStatus().equals("success")) {
                return "success";
            } else {
                return "failure";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "failure";
        }
    }
}
