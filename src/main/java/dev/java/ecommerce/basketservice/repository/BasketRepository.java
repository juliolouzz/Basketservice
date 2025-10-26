package dev.java.ecommerce.basketservice.repository;

import dev.java.ecommerce.basketservice.entity.Basket;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BasketRepository extends MongoRepository<Basket, String> {
}
