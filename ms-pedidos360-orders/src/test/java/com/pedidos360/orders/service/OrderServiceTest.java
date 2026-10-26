package com.pedidos360.orders.service;

import com.pedidos360.orders.dto.OrderItemRequestDto;
import com.pedidos360.orders.dto.OrderRequestDto;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderItem;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.repository.OrderRepository;
import com.pedidos360.orders.service.exception.OrderNotFoundException;
import com.pedidos360.orders.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleOrder = new Order("CLI-123");
        sampleOrder.setId(1L);
        sampleOrder.setEstado(OrderStatus.CREADO);
        sampleOrder.setFechaCreacion(LocalDateTime.now());
        sampleOrder.setFechaActualizacion(LocalDateTime.now());

        OrderItem item1 = new OrderItem("Item A", 2, new BigDecimal("10.00"));
        item1.setId(10L);
        sampleOrder.addItem(item1);
    }

    @Test
    @DisplayName("Crear pedido exitosamente con total calculado y estado CREADO")
    void testCreateOrder() {
        OrderRequestDto request = new OrderRequestDto("CLI-123", List.of(
                new OrderItemRequestDto("Item A", 2, new BigDecimal("10.00")),
                new OrderItemRequestDto("Item B", 1, new BigDecimal("5.50"))
        ));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(1L);
            o.setFechaCreacion(LocalDateTime.now());
            o.setFechaActualizacion(LocalDateTime.now());
            return o;
        });

        OrderResponseDto response = orderService.createOrder(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CLI-123", response.getClienteId());
        assertEquals(OrderStatus.CREADO, response.getEstado());
        assertEquals(new BigDecimal("25.50"), response.getTotal());
        assertEquals(2, response.getItems().size());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderEventPublisher, times(1)).publishOrderCreatedEvents(any(Order.class));
    }

    @Test
    @DisplayName("Crear pedido sin items lanza excepción")
    void testCreateOrderWithoutItems() {
        OrderRequestDto request = new OrderRequestDto("CLI-123", List.of());
        assertThrows(IllegalArgumentException.class, () -> orderService.createOrder(request));
    }

    @Test
    @DisplayName("Consultar pedido por ID existente")
    void testGetOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));

        OrderResponseDto response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("CLI-123", response.getClienteId());
        assertEquals(OrderStatus.CREADO, response.getEstado());
        assertEquals(new BigDecimal("20.00"), response.getTotal());
    }

    @Test
    @DisplayName("Consultar pedido por ID inexistente lanza OrderNotFoundException")
    void testGetOrderByIdNotFound() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    @DisplayName("Actualizar estado de pedido invoca OrderStateMachine")
    void testUpdateOrderStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        doNothing().when(orderStateMachine).validateTransition(OrderStatus.CREADO, OrderStatus.ACEPTADO);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto response = orderService.updateOrderStatus(1L, OrderStatus.ACEPTADO);

        assertNotNull(response);
        assertEquals(OrderStatus.ACEPTADO, response.getEstado());
        verify(orderStateMachine, times(1)).validateTransition(OrderStatus.CREADO, OrderStatus.ACEPTADO);
        verify(orderRepository, times(1)).save(sampleOrder);
        verify(orderEventPublisher, times(1)).publishOrderStatusChangedEvent(sampleOrder, OrderStatus.CREADO, OrderStatus.ACEPTADO);
    }
}
