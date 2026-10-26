package com.pedidos360.orders.service.impl;

import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.service.OrderStateMachine;
import com.pedidos360.orders.service.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class OrderStateMachineImpl implements OrderStateMachine {

    private final Map<OrderStatus, Set<OrderStatus>> transicionesPermitidas;

    public OrderStateMachineImpl() {
        Map<OrderStatus, Set<OrderStatus>> transiciones = new EnumMap<>(OrderStatus.class);

        // CREADO -> ACEPTADO o CANCELADO
        transiciones.put(OrderStatus.CREADO, EnumSet.of(OrderStatus.ACEPTADO, OrderStatus.CANCELADO));

        // ACEPTADO -> EN_PREPARACION o CANCELADO
        transiciones.put(OrderStatus.ACEPTADO, EnumSet.of(OrderStatus.EN_PREPARACION, OrderStatus.CANCELADO));

        // EN_PREPARACION -> DESPACHADO o CANCELADO
        transiciones.put(OrderStatus.EN_PREPARACION, EnumSet.of(OrderStatus.DESPACHADO, OrderStatus.CANCELADO));

        // DESPACHADO -> ENTREGADO o CANCELADO
        transiciones.put(OrderStatus.DESPACHADO, EnumSet.of(OrderStatus.ENTREGADO, OrderStatus.CANCELADO));

        // ENTREGADO es estado terminal: no admite modificaciones ni cancelaciones
        transiciones.put(OrderStatus.ENTREGADO, Collections.emptySet());

        // CANCELADO es estado terminal
        transiciones.put(OrderStatus.CANCELADO, Collections.emptySet());

        this.transicionesPermitidas = Collections.unmodifiableMap(transiciones);
    }

    @Override
    public void validateTransition(OrderStatus estadoActual, OrderStatus nuevoEstado) {
        if (estadoActual == null || nuevoEstado == null) {
            throw new IllegalArgumentException("Los estados actual y nuevo no pueden ser nulos.");
        }

        if (estadoActual == OrderStatus.ENTREGADO) {
            throw new InvalidStateTransitionException(estadoActual, nuevoEstado,
                    "Un pedido en estado ENTREGADO es terminal y no permite ninguna modificación de estado.");
        }

        if (estadoActual == OrderStatus.CANCELADO) {
            throw new InvalidStateTransitionException(estadoActual, nuevoEstado,
                    "Un pedido en estado CANCELADO es terminal y no permite transiciones adicionales.");
        }

        if (estadoActual == nuevoEstado) {
            throw new InvalidStateTransitionException(estadoActual, nuevoEstado,
                    "El pedido ya se encuentra en el estado " + estadoActual + ".");
        }

        Set<OrderStatus> permitidos = getAllowedTransitions(estadoActual);
        if (!permitidos.contains(nuevoEstado)) {
            throw new InvalidStateTransitionException(estadoActual, nuevoEstado, permitidos);
        }
    }

    @Override
    public boolean isValidTransition(OrderStatus estadoActual, OrderStatus nuevoEstado) {
        if (estadoActual == null || nuevoEstado == null || estadoActual == nuevoEstado) {
            return false;
        }
        return getAllowedTransitions(estadoActual).contains(nuevoEstado);
    }

    @Override
    public Set<OrderStatus> getAllowedTransitions(OrderStatus estadoActual) {
        if (estadoActual == null) {
            return Collections.emptySet();
        }
        return transicionesPermitidas.getOrDefault(estadoActual, Collections.emptySet());
    }
}
