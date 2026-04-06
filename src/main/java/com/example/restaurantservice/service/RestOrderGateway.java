package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.CreateRestaurantOrderRequest;
import com.example.restaurantservice.dto.OrderResponse;
import com.example.restaurantservice.dto.UpdateRestaurantOrderRequest;
import java.util.Arrays;
import java.util.List;
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

    @Override
    public List<OrderResponse> getOrders(Long restaurantId) {
        OrderResponse[] response = orderRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/orders")
                        .queryParam("restaurantId", restaurantId)
                        .build())
                .retrieve()
                .body(OrderResponse[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
        return orderRestClient.get()
                .uri("/api/orders/{id}", orderId)
                .retrieve()
                .body(OrderResponse.class);
    }

    @Override
    public OrderResponse updateOrder(Long orderId, UpdateRestaurantOrderRequest request) {
        return orderRestClient.put()
                .uri("/api/orders/{id}", orderId)
                .body(request)
                .retrieve()
                .body(OrderResponse.class);
    }

    private record SaveOrderRequest(Long restaurantId, String customerName, String itemName, Integer quantity) {
    }
}
