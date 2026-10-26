package com.pedidos360.orders.dto;

import com.pedidos360.orders.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Solicitud para actualizar el estado de un pedido")
public class UpdateStatusRequestDto {

    @Schema(description = "Nuevo estado a asignar al pedido", example = "ACEPTADO", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El nuevo estado es obligatorio")
    private OrderStatus nuevoEstado;

    public UpdateStatusRequestDto() {
    }

    public UpdateStatusRequestDto(OrderStatus nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public OrderStatus getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(OrderStatus nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }
}
