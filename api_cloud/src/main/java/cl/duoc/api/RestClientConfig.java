package cl.duoc.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient ordersServiceClient(
        @Value("${microservice.orders-url:${microservice.base-url}}") String baseUrl
    ) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean
    RestClient catalogServiceClient(
        @Value("${microservice.catalog-url:http://localhost:8086}") String catalogUrl
    ) {
        return RestClient.builder().baseUrl(catalogUrl).build();
    }

    @Bean
    com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}