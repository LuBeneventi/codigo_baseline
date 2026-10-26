package com.pedidos360.orders.service;

import com.pedidos360.orders.entity.OrderStatus;

import java.util.Set;

public interface OrderStateMachine {

    void validateTransition(OrderStatus estadoActual, OrderStatus nuevoEstado);

    boolean isValidTransition(OrderStatus estadoActual, OrderStatus nuevoEstado);

    Set<OrderStatus> getAllowedTransitions(OrderStatus estadoActual);
}
