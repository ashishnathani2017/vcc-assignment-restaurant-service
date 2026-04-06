package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;

public interface OrderGateway {

    OrderResponse saveOrder(Long restaurantId, CreateRestaurantOrderRequest request);
}
