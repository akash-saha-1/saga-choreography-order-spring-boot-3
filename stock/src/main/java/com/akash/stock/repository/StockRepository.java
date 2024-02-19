package com.akash.stock.repository;

import com.akash.stock.entity.WareHouse;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends CrudRepository<WareHouse, Long> {

    Iterable<WareHouse> findByItem(String item);
}
