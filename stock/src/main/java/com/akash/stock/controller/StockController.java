package com.akash.stock.controller;

import com.akash.stock.dto.CustomerOrder;
import com.akash.stock.dto.OrderEvent;
import com.akash.stock.dto.Stock;
import com.akash.stock.entity.WareHouse;
import com.akash.stock.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class StockController {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @KafkaListener(topics = "new-payment", groupId = "payments-group")
    public void updateStock(String paymentEvent) throws JsonMappingException, JsonProcessingException {
        System.out.println("Inside update inventory for order "+paymentEvent);

        OrderEvent event = new OrderEvent();

        OrderEvent stockOrderEvent = new ObjectMapper().readValue(paymentEvent, OrderEvent.class);
        CustomerOrder order = stockOrderEvent.getCustomerOrder();

        try {
            Iterable<WareHouse> inventories = stockRepository.findByItem(order.getItem());

            boolean exists = inventories.iterator().hasNext();

            if (!exists) {
                System.out.println("Stock not exist so reverting the order");
                throw new RuntimeException("Stock not available");
            }

            inventories.forEach(i -> {
                if(i.getQuantity() < order.getQuantity()) {
                    try {
                        throw new RuntimeException("Stock not available");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                i.setQuantity(i.getQuantity() - order.getQuantity());

                stockRepository.save(i);
            });

            event.setType("STOCK_UPDATED");
            event.setCustomerOrder(stockOrderEvent.getCustomerOrder());
            kafkaTemplate.send("new-stock", event);
            System.out.println("sent to delivery");
        } catch (Exception e) {
            OrderEvent pe = new OrderEvent();
            pe.setCustomerOrder(order);
            pe.setType("PAYMENT_REVERSED");
            kafkaTemplate.send("reversed-payments", pe);
            e.printStackTrace();
        }
    }

    @PostMapping("/addItems")
    public HttpStatus addItems(@RequestBody Stock stock) {
        Iterable<WareHouse> items = stockRepository.findByItem(stock.getItem());

        if (items.iterator().hasNext()) {
            items.forEach(i -> {
                i.setQuantity(stock.getQuantity() + i.getQuantity());
                stockRepository.save(i);
            });
        } else {
            WareHouse wareHouse = new WareHouse();
            wareHouse.setItem(stock.getItem());
            wareHouse.setQuantity(stock.getQuantity());
            stockRepository.save(wareHouse);
        }
        return HttpStatus.ACCEPTED;
    }
}
