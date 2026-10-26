package com.pedidos360.orders.service;

import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.service.exception.InvalidStateTransitionException;
import com.pedidos360.orders.service.impl.OrderStateMachineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachineImpl();
    }

    @Test
    @DisplayName("Transición normal completa: CREADO -> ACEPTADO -> EN_PREPARACION -> DESPACHADO -> ENTREGADO")
    void testSequentialHappyPath() {
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.CREADO, OrderStatus.ACEPTADO));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.ACEPTADO, OrderStatus.EN_PREPARACION));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.EN_PREPARACION, OrderStatus.DESPACHADO));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.DESPACHADO, OrderStatus.ENTREGADO));
    }

    @Test
    @DisplayName("Cancelación permitida desde estados activos")
    void testCancelFromActiveStates() {
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.CREADO, OrderStatus.CANCELADO));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.ACEPTADO, OrderStatus.CANCELADO));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.EN_PREPARACION, OrderStatus.CANCELADO));
        assertDoesNotThrow(() -> stateMachine.validateTransition(OrderStatus.DESPACHADO, OrderStatus.CANCELADO));
    }

    @Test
    @DisplayName("Transición inválida: saltar de CREADO a DESPACHADO sin aceptar ni preparar")
    void testCannotSkipStages() {
        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(OrderStatus.CREADO, OrderStatus.DESPACHADO)
        );
        assertEquals(OrderStatus.CREADO, ex.getEstadoActual());
        assertEquals(OrderStatus.DESPACHADO, ex.getNuevoEstado());
        assertTrue(ex.getMessage().contains("No es posible cambiar el estado de CREADO a DESPACHADO"));
    }

    @Test
    @DisplayName("Transición inválida: despachar sin antes aceptar")
    void testCannotDispatchBeforeAccept() {
        assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(OrderStatus.CREADO, OrderStatus.DESPACHADO)
        );
    }

    @Test
    @DisplayName("Transición inválida: no se puede cancelar un pedido ya ENTREGADO")
    void testCannotCancelDeliveredOrder() {
        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(OrderStatus.ENTREGADO, OrderStatus.CANCELADO)
        );
        assertEquals(OrderStatus.ENTREGADO, ex.getEstadoActual());
        assertEquals(OrderStatus.CANCELADO, ex.getNuevoEstado());
        assertTrue(ex.getMessage().contains("ENTREGADO es terminal y no permite ninguna modificación"));
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("Transición inválida: ENTREGADO no admite transición a ningún estado")
    void testDeliveredIsTerminal(OrderStatus target) {
        assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(OrderStatus.ENTREGADO, target)
        );
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("Transición inválida: CANCELADO no admite transición a ningún estado")
    void testCancelledIsTerminal(OrderStatus target) {
        assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(OrderStatus.CANCELADO, target)
        );
    }

    @Test
    @DisplayName("Transición hacia el mismo estado es rechazada")
    void testSameStateTransitionRejected() {
        InvalidStateTransitionException ex = assertThrows(
                InvalidStateTransitionException.class,
                () -> stateMachine.validateTransition(OrderStatus.CREADO, OrderStatus.CREADO)
        );
        assertTrue(ex.getMessage().contains("ya se encuentra en el estado CREADO"));
    }

    @Test
    @DisplayName("Verificación de transiciones permitidas")
    void testGetAllowedTransitions() {
        Set<OrderStatus> fromCreado = stateMachine.getAllowedTransitions(OrderStatus.CREADO);
        assertEquals(Set.of(OrderStatus.ACEPTADO, OrderStatus.CANCELADO), fromCreado);

        Set<OrderStatus> fromEntregado = stateMachine.getAllowedTransitions(OrderStatus.ENTREGADO);
        assertTrue(fromEntregado.isEmpty());
    }
}
