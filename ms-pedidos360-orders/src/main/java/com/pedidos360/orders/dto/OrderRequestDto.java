package com.pedidos360.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "Datos para el registro de un nuevo pedido")
public class OrderRequestDto {

    @Schema(description = "Identificador del cliente", example = "CLI-98234")
    @NotBlank(message = "El clienteId es obligatorio")
    private String clienteId;

    @Schema(description = "Nombre del cliente (opcional)", example = "Sofía Morales")
    private String clienteNombre;

    @Schema(description = "Correo electrónico del cliente para notificaciones y facturación", example = "sofia.morales@example.com")
    private String clienteEmail;

    @Schema(description = "Dirección de despacho o facturación del cliente", example = "Av. Providencia 1234, Santiago")
    private String clienteDireccion;

    @Schema(description = "RUT / Identificación tributaria del cliente", example = "18.765.432-1")
    private String clienteRut;

    @Schema(description = "Lista de ítems que componen el pedido (al menos uno requerido)")
    @NotEmpty(message = "El pedido debe contener al menos un ítem")
    @Valid
    private List<OrderItemRequestDto> items = new ArrayList<>();

    public OrderRequestDto() {
    }

    public OrderRequestDto(String clienteId, List<OrderItemRequestDto> items) {
        this.clienteId = clienteId;
        this.items = items;
    }

    public OrderRequestDto(String clienteId, String clienteNombre, String clienteEmail, List<OrderItemRequestDto> items) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.items = items;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public String getClienteDireccion() {
        return clienteDireccion;
    }

    public void setClienteDireccion(String clienteDireccion) {
        this.clienteDireccion = clienteDireccion;
    }

    public String getClienteRut() {
        return clienteRut;
    }

    public void setClienteRut(String clienteRut) {
        this.clienteRut = clienteRut;
    }

    public List<OrderItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDto> items) {
        this.items = items;
    }
}
