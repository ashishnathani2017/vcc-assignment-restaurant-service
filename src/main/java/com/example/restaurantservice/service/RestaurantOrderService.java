package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;
import org.springframework.stereotype.Service;

@Service
public class RestaurantOrderService {

    private final RestaurantService restaurantService;
    private final OrderGateway orderGateway;

    public RestaurantOrderService(RestaurantService restaurantService, OrderGateway orderGateway) {
        this.restaurantService = restaurantService;
        this.orderGateway = orderGateway;
    }

    public OrderResponse handleRestaurantOrder(Long restaurantId, CreateRestaurantOrderRequest request) {
        restaurantService.getRestaurantById(restaurantId);
        return orderGateway.saveOrder(restaurantId, request);
    }
}
