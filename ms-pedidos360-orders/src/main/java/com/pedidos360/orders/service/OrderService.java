package com.pedidos360.orders.service;

import com.pedidos360.orders.dto.OrderRequestDto;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto request);

    OrderResponseDto getOrderById(Long id);

    OrderResponseDto updateOrderStatus(Long id, OrderStatus nuevoEstado);

    List<OrderResponseDto> listOrders(OrderStatus status, LocalDateTime from, LocalDateTime to);
}
