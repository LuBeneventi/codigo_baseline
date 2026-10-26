package com.pedidos360.orders.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.orders.controller.exception.GlobalExceptionHandler;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.dto.UpdateStatusRequestDto;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.service.OrderService;
import com.pedidos360.orders.service.exception.InvalidStateTransitionException;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Transición válida CREADO a ACEPTADO responde 200 OK")
    void testValidTransitionToAceptado() throws Exception {
        UpdateStatusRequestDto request = new UpdateStatusRequestDto(OrderStatus.ACEPTADO);
        OrderResponseDto response = new OrderResponseDto(1L, "CLI-01", List.of(), new BigDecimal("10.00"),
                OrderStatus.ACEPTADO, LocalDateTime.now(), LocalDateTime.now());

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.ACEPTADO))).thenReturn(response);

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACEPTADO"));
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Intentar despachar sin aceptar responde 409 Conflict")
    void testInvalidTransitionDispatchWithoutAccept() throws Exception {
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
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No es posible cambiar el estado de CREADO a DESPACHADO")));
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Intentar modificar pedido ENTREGADO responde 409 Conflict")
    void testCannotModifyDeliveredOrder() throws Exception {
        UpdateStatusRequestDto request = new UpdateStatusRequestDto(OrderStatus.CANCELADO);

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CANCELADO)))
                .thenThrow(new InvalidStateTransitionException(
                        OrderStatus.ENTREGADO,
                        OrderStatus.CANCELADO,
                        "Un pedido en estado ENTREGADO es terminal y no permite ninguna modificación de estado."
                ));

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ENTREGADO es terminal")));
    }
}
