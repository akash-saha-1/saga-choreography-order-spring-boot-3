package com.akash.payment.service;

import com.akash.payment.dto.CustomerOrder;
import com.akash.payment.dto.OrderEvent;
import com.akash.payment.entity.Payment;
import com.akash.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Optional;

@Service
public class ReversePayment {
    @Autowired
    private PaymentRepository paymentRepository;

    @KafkaListener(topics = "revered-payments", groupId = "payments-group")
    public void reversePayment(String event) {
        System.out.println("Reverse Payment event :: " + event);

        try {
            OrderEvent orderEvent = new ObjectMapper().readValue(event, OrderEvent.class);
            CustomerOrder customerOrder = orderEvent.getCustomerOrder();

            Iterable<Payment> payments = paymentRepository.findByOrderId(customerOrder.getOrderId());
            payments.forEach(p -> {
                p.setStatus("Failed");
                paymentRepository.save(p);
            });
        } catch (Exception e) {
            System.out.println("Exception occured while reverting order details: " + event);
        }
    }
}

