package com.example.restaurantservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrderClientConfig {

    @Bean
    public RestClient orderRestClient(@Value("${order.service.base-url}") String orderServiceBaseUrl) {
        return RestClient.builder()
                .baseUrl(orderServiceBaseUrl)
                .build();
    }
}
