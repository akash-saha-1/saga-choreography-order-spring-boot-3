package com.akash.payment.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CustomerOrder {
    private double amount;
    private String item;
    private int quantity;
    private String paymentMethod;
    private long orderId;
    private String address;
}
