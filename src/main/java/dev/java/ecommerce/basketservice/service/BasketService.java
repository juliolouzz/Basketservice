package dev.java.ecommerce.basketservice.service;

import dev.java.ecommerce.basketservice.client.response.PlatziProductResponse;
import dev.java.ecommerce.basketservice.controller.request.BasketRequest;
import dev.java.ecommerce.basketservice.controller.request.PaymentRequest;
import dev.java.ecommerce.basketservice.entity.Basket;
import dev.java.ecommerce.basketservice.entity.Product;
import dev.java.ecommerce.basketservice.entity.Status;
import dev.java.ecommerce.basketservice.exceptions.BusinessException;
import dev.java.ecommerce.basketservice.exceptions.DataNotFoundException;
import dev.java.ecommerce.basketservice.repository.BasketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BasketService {

    private final BasketRepository basketRepository;
    private final ProductService productService;

    public Basket getBasketById(String id) {
        return basketRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Basket not found"));
    }

    public Basket createBasket(BasketRequest basketRequest) {

        basketRepository.findByClientAndStatus(basketRequest.clientId(), Status.OPEN)
                .ifPresent(existingBasket -> {
                    throw new BusinessException("Client already has an open basket");
                });

        List<Product> products = getProducts(basketRequest);

        Basket basket = Basket.builder()
                .client(basketRequest.clientId())
                .status(Status.OPEN)
                .products(products)
                .build();

        basket.calculateTotalPrice();
        return basketRepository.save(basket);
    }

    private List<Product> getProducts(BasketRequest basketRequest) {
        List<Product> products = new ArrayList<>();
        basketRequest.products().forEach(productRequest -> {
            PlatziProductResponse platziProductResponse = productService.getProductById(productRequest.id());

            products.add(Product.builder()
                    .id(platziProductResponse.id())
                    .title(platziProductResponse.title())
                    .price(platziProductResponse.price())
                    .quantity(productRequest.quantity())
                    .build());

        });
        return products;
    }

    public Basket updateBasket(String basketId, BasketRequest request) {
        Basket existingBasket = getBasketById(basketId);

        if (existingBasket.getStatus() != Status.OPEN) {
            throw new IllegalStateException("Only open baskets can be updated");
        }

        List<Product> products = getProducts(request);

        existingBasket.setProducts(products);
        existingBasket.calculateTotalPrice();
        return basketRepository.save(existingBasket);
    }

    public Basket payBasket(String basketId, PaymentRequest request) {
        Basket existingBasket = getBasketById(basketId);
        if (existingBasket.getStatus() != Status.OPEN) {
            throw new IllegalStateException("Only open baskets can be paid");
        }
        existingBasket.setPaymentMethod(request.getPaymentMethod());
        existingBasket.setStatus(Status.SOLD);
        return basketRepository.save(existingBasket);
    }

    public void deleteBasket(String basketId) {
        basketRepository.delete(getBasketById(basketId));
    }

}
