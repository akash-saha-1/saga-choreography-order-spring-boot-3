package com.akash.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderEvent {
    private CustomerOrder customerOrder;
    private String type;
}
