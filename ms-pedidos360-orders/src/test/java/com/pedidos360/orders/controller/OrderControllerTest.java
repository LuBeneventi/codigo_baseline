package com.pedidos360.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.orders.controller.exception.GlobalExceptionHandler;
import com.pedidos360.orders.dto.OrderItemRequestDto;
import com.pedidos360.orders.dto.OrderItemResponseDto;
import com.pedidos360.orders.dto.OrderRequestDto;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.dto.UpdateStatusRequestDto;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.service.OrderService;
import com.pedidos360.orders.service.exception.InvalidStateTransitionException;
import com.pedidos360.orders.service.exception.OrderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /api/orders retorna 201 Created al registrar un pedido válido")
    void testCreateOrderSuccess() throws Exception {
        OrderRequestDto request = new OrderRequestDto("CLI-100", List.of(
                new OrderItemRequestDto("Pizza Pepperoni", 2, new BigDecimal("12.50"))
        ));

        OrderResponseDto response = new OrderResponseDto(
                1L,
                "CLI-100",
                List.of(new OrderItemResponseDto(10L, "Pizza Pepperoni", 2, new BigDecimal("12.50"), new BigDecimal("25.00"))),
                new BigDecimal("25.00"),
                OrderStatus.CREADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.createOrder(any(OrderRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clienteId").value("CLI-100"))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.total").value(25.00));
    }

    @Test
    @DisplayName("POST /api/orders retorna 400 Bad Request si los datos son inválidos")
    void testCreateOrderInvalid() throws Exception {
        OrderRequestDto invalidRequest = new OrderRequestDto("", List.of());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} retorna 200 OK cuando existe el pedido")
    void testGetOrderByIdSuccess() throws Exception {
        OrderResponseDto response = new OrderResponseDto(
                1L,
                "CLI-100",
                List.of(),
                new BigDecimal("25.00"),
                OrderStatus.CREADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.getOrderById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clienteId").value("CLI-100"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} retorna 404 Not Found cuando el pedido no existe")
    void testGetOrderByIdNotFound() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Pedido con identificador 999 no encontrado."));
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status retorna 200 OK ante transición válida")
    void testUpdateStatusSuccess() throws Exception {
        UpdateStatusRequestDto request = new UpdateStatusRequestDto(OrderStatus.ACEPTADO);

        OrderResponseDto response = new OrderResponseDto(
                1L,
                "CLI-100",
                List.of(),
                new BigDecimal("25.00"),
                OrderStatus.ACEPTADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.ACEPTADO))).thenReturn(response);

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ACEPTADO"));
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status retorna 409 Conflict ante transición de estado inválida")
    void testUpdateStatusConflict() throws Exception {
        UpdateStatusRequestDto request = new UpdateStatusRequestDto(OrderStatus.DESPACHADO);

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.DESPACHADO)))
                .thenThrow(new InvalidStateTransitionException(
                        OrderStatus.CREADO,
                        OrderStatus.DESPACHADO,
                        Set.of(OrderStatus.ACEPTADO, OrderStatus.CANCELADO)
                ));

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Transición inválida")));
    }

    @Test
    @DisplayName("GET /api/orders retorna 200 OK con filtros")
    void testListOrdersWithFilter() throws Exception {
        OrderResponseDto response = new OrderResponseDto(
                1L,
                "CLI-100",
                List.of(),
                new BigDecimal("25.00"),
                OrderStatus.CREADO,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(orderService.listOrders(eq(OrderStatus.CREADO), any(), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders?status=CREADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].estado").value("CREADO"));
    }
}
