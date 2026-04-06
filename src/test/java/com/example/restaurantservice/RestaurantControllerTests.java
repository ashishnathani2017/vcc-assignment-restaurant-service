package com.example.restaurantservice;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurantservice.dto.OrderResponse;
import com.example.restaurantservice.service.OrderGateway;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RestaurantControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderGateway orderGateway;

    @Test
    void shouldManageRestaurantCrud() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Spice Garden",
                                  "address": "MG Road",
                                  "cuisine": "Indian"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Spice Garden"));

        mockMvc.perform(put("/api/restaurants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Spice Garden Prime",
                                  "address": "Brigade Road",
                                  "cuisine": "North Indian"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Spice Garden Prime"));

        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("Brigade Road"));

        mockMvc.perform(delete("/api/restaurants/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/restaurants/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleRestaurantOrderAndDelegateSaving() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Urban Tandoor",
                                  "address": "Indiranagar",
                                  "cuisine": "Indian"
                                }
                                """))
                .andExpect(status().isCreated());

        OrderResponse response = new OrderResponse();
        response.setId(101L);
        response.setRestaurantId(1L);
        response.setCustomerName("Asha");
        response.setItemName("Paneer Tikka");
        response.setQuantity(2);
        response.setStatus("SAVED");

        when(orderGateway.saveOrder(ArgumentMatchers.eq(1L), ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/restaurants/1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Asha",
                                  "itemName": "Paneer Tikka",
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.status").value("SAVED"));

        verify(orderGateway).saveOrder(ArgumentMatchers.eq(1L), ArgumentMatchers.any());
    }

    @Test
    void shouldViewUpdateAndListRestaurantOrders() throws Exception {
        mockMvc.perform(post("/api/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blue Pepper",
                                  "address": "Koramangala",
                                  "cuisine": "Italian"
                                }
                                """))
                .andExpect(status().isCreated());

        OrderResponse existing = new OrderResponse();
        existing.setId(55L);
        existing.setRestaurantId(1L);
        existing.setCustomerName("Neha");
        existing.setItemName("Pasta");
        existing.setQuantity(1);
        existing.setStatus("SAVED");

        OrderResponse updated = new OrderResponse();
        updated.setId(55L);
        updated.setRestaurantId(1L);
        updated.setCustomerName("Neha Singh");
        updated.setItemName("White Sauce Pasta");
        updated.setQuantity(2);
        updated.setStatus("CONFIRMED");

        when(orderGateway.getOrders(1L)).thenReturn(List.of(existing));
        when(orderGateway.getOrder(55L)).thenReturn(existing, existing);
        when(orderGateway.updateOrder(ArgumentMatchers.eq(55L), ArgumentMatchers.any())).thenReturn(updated);

        mockMvc.perform(get("/api/restaurants/1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(55))
                .andExpect(jsonPath("$[0].itemName").value("Pasta"));

        mockMvc.perform(get("/api/restaurants/1/orders/55"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Neha"));

        mockMvc.perform(put("/api/restaurants/1/orders/55")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Neha Singh",
                                  "itemName": "White Sauce Pasta",
                                  "quantity": 2,
                                  "status": "CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Neha Singh"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(orderGateway).getOrders(1L);
        verify(orderGateway, org.mockito.Mockito.times(2)).getOrder(55L);
        verify(orderGateway).updateOrder(ArgumentMatchers.eq(55L), ArgumentMatchers.any());
        verifyNoMoreInteractions(orderGateway);
    }
}
