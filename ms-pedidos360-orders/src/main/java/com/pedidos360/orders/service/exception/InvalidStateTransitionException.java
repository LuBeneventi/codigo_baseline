package com.pedidos360.orders.service.exception;

import com.pedidos360.orders.entity.OrderStatus;

import java.util.Set;

public class InvalidStateTransitionException extends RuntimeException {

    private final OrderStatus estadoActual;
    private final OrderStatus nuevoEstado;

    public InvalidStateTransitionException(OrderStatus estadoActual, OrderStatus nuevoEstado, String motivo) {
        super(String.format("Transición inválida: No es posible cambiar el estado de %s a %s. %s",
                estadoActual, nuevoEstado, motivo));
        this.estadoActual = estadoActual;
        this.nuevoEstado = nuevoEstado;
    }

    public InvalidStateTransitionException(OrderStatus estadoActual, OrderStatus nuevoEstado, Set<OrderStatus> permitidos) {
        super(String.format("Transición inválida: No es posible cambiar el estado de %s a %s. Estados permitidos desde %s: %s",
                estadoActual, nuevoEstado, estadoActual, permitidos.isEmpty() ? "Ninguno (estado terminal)" : permitidos));
        this.estadoActual = estadoActual;
        this.nuevoEstado = nuevoEstado;
    }

    public OrderStatus getEstadoActual() {
        return estadoActual;
    }

    public OrderStatus getNuevoEstado() {
        return nuevoEstado;
    }
}
