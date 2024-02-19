package com.akash.payment.controller;

import com.akash.payment.dto.CustomerOrder;
import com.akash.payment.dto.OrderEvent;
import com.akash.payment.entity.Payment;
import com.akash.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class PaymentController {
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @KafkaListener(topics = "new-order", groupId = "orders-group")
    public void processPayment(String event) throws JsonProcessingException {
        System.out.println("Process payment event: " + event);
        OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);
        CustomerOrder customerOrder = orderEvent.getCustomerOrder();

        Payment payment = new Payment();
        payment.setAmount(customerOrder.getAmount());
        payment.setMode(customerOrder.getPaymentMethod());
        payment.setOrderId(customerOrder.getOrderId());
        payment.setStatus("Success");

        try {
            paymentRepository.save(payment);

            OrderEvent orderEventPayment = new OrderEvent();
            orderEventPayment.setCustomerOrder(customerOrder);
            orderEventPayment.setType("PAYMENT CREATED");

            kafkaTemplate.send("new-payment", orderEventPayment);
            System.out.println("sent to stock");
        } catch(Exception e) {
            payment.setStatus("Failed");
            paymentRepository.save(payment);

            OrderEvent orderEventReversed = new OrderEvent();
            orderEventReversed.setCustomerOrder(customerOrder);
            orderEventReversed.setType("ORDER_REVERSED");

            kafkaTemplate.send("reversed-order", orderEventReversed);
            System.out.println("Exception Occured while process New Payment: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
