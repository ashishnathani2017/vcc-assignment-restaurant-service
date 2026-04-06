package com.example.restaurantservice;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.restaurantservice.dto.OrderResponse;
import com.example.restaurantservice.service.OrderGateway;
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
}
