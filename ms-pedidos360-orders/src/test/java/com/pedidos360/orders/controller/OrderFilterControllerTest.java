package com.pedidos360.orders.controller;

import com.pedidos360.orders.controller.exception.GlobalExceptionHandler;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderFilterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("GET /api/orders con filtro de estado retorna 200 OK")
    void testFilterByStatus() throws Exception {
        OrderResponseDto dto = new OrderResponseDto(1L, "CLI-01", List.of(), new BigDecimal("10.00"),
                OrderStatus.CREADO, LocalDateTime.now(), LocalDateTime.now());

        when(orderService.listOrders(eq(OrderStatus.CREADO), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/orders?status=CREADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("CREADO"));
    }

    @Test
    @DisplayName("GET /api/orders con rango de fechas inconsistente retorna 400 Bad Request")
    void testInvalidDateRangeReturnsBadRequest() throws Exception {
        when(orderService.listOrders(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("La fecha inicial ('from') no puede ser posterior a la fecha final ('to')."));

        mockMvc.perform(get("/api/orders?from=2026-09-30T00:00:00&to=2026-09-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no puede ser posterior")));
    }
}
