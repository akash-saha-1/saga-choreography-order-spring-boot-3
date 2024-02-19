package com.akash.delivery.repository;

import com.akash.delivery.entity.Delivery;
import org.springframework.data.repository.CrudRepository;

public interface DeliveryRepository extends CrudRepository<Delivery, Long> {
    Delivery findByOrderId(long orderId);
}
