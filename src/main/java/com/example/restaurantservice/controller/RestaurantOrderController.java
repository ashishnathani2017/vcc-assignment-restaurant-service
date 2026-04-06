package com.example.restaurantservice.controller;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;
import com.example.restaurantservice.dto.UpdateRestaurantOrderRequest;
import com.example.restaurantservice.service.RestaurantOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants/{restaurantId}/orders")
public class RestaurantOrderController {

    private final RestaurantOrderService restaurantOrderService;

    public RestaurantOrderController(RestaurantOrderService restaurantOrderService) {
        this.restaurantOrderService = restaurantOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@PathVariable Long restaurantId,
                                    @Valid @RequestBody CreateRestaurantOrderRequest request) {
        return restaurantOrderService.handleRestaurantOrder(restaurantId, request);
    }

    @GetMapping
    public List<OrderResponse> getOrders(@PathVariable Long restaurantId) {
        return restaurantOrderService.getOrders(restaurantId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long restaurantId, @PathVariable Long orderId) {
        return restaurantOrderService.getOrder(restaurantId, orderId);
    }

    @PutMapping("/{orderId}")
    public OrderResponse updateOrder(@PathVariable Long restaurantId,
                                     @PathVariable Long orderId,
                                     @Valid @RequestBody UpdateRestaurantOrderRequest request) {
        return restaurantOrderService.updateOrder(restaurantId, orderId, request);
    }
}
