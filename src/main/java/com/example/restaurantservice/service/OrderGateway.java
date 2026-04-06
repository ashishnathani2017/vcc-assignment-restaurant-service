package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;
import com.example.restaurantservice.dto.UpdateRestaurantOrderRequest;
import java.util.List;

public interface OrderGateway {

    OrderResponse saveOrder(Long restaurantId, CreateRestaurantOrderRequest request);

    List<OrderResponse> getOrders(Long restaurantId);

    OrderResponse getOrder(Long orderId);

    OrderResponse updateOrder(Long orderId, UpdateRestaurantOrderRequest request);
}
