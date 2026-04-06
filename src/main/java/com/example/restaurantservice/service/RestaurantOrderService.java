package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;
import com.example.restaurantservice.dto.UpdateRestaurantOrderRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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

    public List<OrderResponse> getOrders(Long restaurantId) {
        restaurantService.getRestaurantById(restaurantId);
        return orderGateway.getOrders(restaurantId);
    }

    public OrderResponse getOrder(Long restaurantId, Long orderId) {
        restaurantService.getRestaurantById(restaurantId);
        OrderResponse order = orderGateway.getOrder(orderId);
        validateRestaurantOwnership(restaurantId, order);
        return order;
    }

    public OrderResponse updateOrder(Long restaurantId, Long orderId, UpdateRestaurantOrderRequest request) {
        restaurantService.getRestaurantById(restaurantId);
        OrderResponse existingOrder = orderGateway.getOrder(orderId);
        validateRestaurantOwnership(restaurantId, existingOrder);
        OrderResponse updatedOrder = orderGateway.updateOrder(orderId, request);
        validateRestaurantOwnership(restaurantId, updatedOrder);
        return updatedOrder;
    }

    private void validateRestaurantOwnership(Long restaurantId, OrderResponse order) {
        if (!restaurantId.equals(order.getRestaurantId())) {
            throw new EntityNotFoundException(
                    "Order " + order.getId() + " does not belong to restaurant " + restaurantId
            );
        }
    }
}
