package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestOrderGateway implements OrderGateway {

    private final RestClient orderRestClient;

    public RestOrderGateway(RestClient orderRestClient) {
        this.orderRestClient = orderRestClient;
    }

    @Override
    public OrderResponse saveOrder(Long restaurantId, CreateRestaurantOrderRequest request) {
        return orderRestClient.post()
                .uri("/api/orders")
                .body(new SaveOrderRequest(restaurantId, request.getCustomerName(), request.getItemName(), request.getQuantity()))
                .retrieve()
                .body(OrderResponse.class);
    }

    private record SaveOrderRequest(Long restaurantId, String customerName, String itemName, Integer quantity) {
    }
}
