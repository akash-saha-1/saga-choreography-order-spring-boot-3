package com.akash.order.repository;

import com.akash.order.enity.OrderTable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends CrudRepository<OrderTable, Long> {
}
