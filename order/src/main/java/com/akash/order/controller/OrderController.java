package com.akash.order.controller;

import com.akash.order.dto.CustomerOrder;
import com.akash.order.dto.OrderEvent;
import com.akash.order.enity.OrderTable;
import com.akash.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Autowired
    RestTemplate restTemplate;

    @PostMapping("/order")
    public ResponseEntity createOrder(@RequestBody CustomerOrder customerOrder) {
        OrderTable order = new OrderTable();
        order.setAmount(customerOrder.getAmount());
        order.setItem(customerOrder.getItem());
        order.setQuantity(customerOrder.getQuantity());
        order.setStatus("Created");

        try {
            order = orderRepository.save(order);
            customerOrder.setOrderId(order.getId());

            OrderEvent orderEvent = new OrderEvent();
            orderEvent.setCustomerOrder(customerOrder);
            orderEvent.setType("ORDER CREATED");

            kafkaTemplate.send("new-order", orderEvent);
            System.out.println("sent to payment");

            String status = "pending";
            int retrievalCount = 0;
            while (status.equals("pending")) {
                if (retrievalCount > 100) {
                    // Order fails if retrieval count exceeds 100
                    order.setStatus("Failed");
                    orderRepository.save(order);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Order Placement Failed - Retrieval Count Exceeded");
                }

                ResponseEntity<String> response = restTemplate.getForEntity(
                        "http://localhost:8083/api/getDeliveryStatusByOrderId/" + order.getId(), String.class);
                status = response.getBody();

                retrievalCount++;
                // If status is still pending, sleep for 2 seconds and check again
                if (status.equals("pending")) {
                    Thread.sleep(2000); // Sleep for 2 seconds
                }
            }

            // Return the appropriate response based on the final status
            if (status.equals("success")) {
                order.setStatus("Success-Delivered");
                orderRepository.save(order);
                return ResponseEntity.status(HttpStatus.CREATED).body("Order Placed Successfully");
            } else {
                order.setStatus("Failed");
                orderRepository.save(order);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order Placement Failed");
            }
        } catch(Exception e) {
            order.setStatus("Failed");
            orderRepository.save(order);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
