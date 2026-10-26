package com.pedidos360.orders.service.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long id) {
        super("Pedido con identificador " + id + " no encontrado.");
    }
}
