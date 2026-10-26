package com.pedidos360.orders.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pedidos360.orders.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "Datos de respuesta con la información completa de un pedido")
public class OrderResponseDto {

    @Schema(description = "Identificador único del pedido", example = "1")
    private Long id;

    @Schema(description = "Identificador del cliente", example = "CLI-98234")
    private String clienteId;

    // BASELINE-VULN [SEC-REQ-007]: expone nombre, correo, rut y direccion del cliente a cualquier consultante
    @Schema(description = "Nombre del cliente", example = "Sofía Morales")
    private String clienteNombre;

    @Schema(description = "Correo electrónico del cliente", example = "sofia.morales@example.com")
    private String clienteEmail;

    @Schema(description = "Dirección de despacho o facturación", example = "Av. Providencia 1234")
    private String clienteDireccion;

    @Schema(description = "RUT del cliente", example = "18.765.432-1")
    private String clienteRut;

    @Schema(description = "Lista de ítems incluidos en el pedido")
    private List<OrderItemResponseDto> items = new ArrayList<>();

    @Schema(description = "Monto total del pedido", example = "25.00")
    private BigDecimal total;

    @Schema(description = "Estado actual del pedido", example = "CREADO")
    private OrderStatus estado;

    @Schema(description = "Fecha y hora de creación", example = "2026-09-03T15:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Fecha y hora de última actualización", example = "2026-09-03T15:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaActualizacion;

    public OrderResponseDto() {
    }

    public OrderResponseDto(Long id, String clienteId, List<OrderItemResponseDto> items, BigDecimal total,
                            OrderStatus estado, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this(id, clienteId, null, null, null, null, items, total, estado, fechaCreacion, fechaActualizacion);
    }

    public OrderResponseDto(Long id, String clienteId, String clienteNombre, String clienteEmail,
                            List<OrderItemResponseDto> items, BigDecimal total,
                            OrderStatus estado, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this(id, clienteId, clienteNombre, clienteEmail, null, null, items, total, estado, fechaCreacion, fechaActualizacion);
    }

    public OrderResponseDto(Long id, String clienteId, String clienteNombre, String clienteEmail,
                            String clienteDireccion, String clienteRut,
                            List<OrderItemResponseDto> items, BigDecimal total,
                            OrderStatus estado, LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.clienteDireccion = clienteDireccion;
        this.clienteRut = clienteRut;
        this.items = items;
        this.total = total;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<OrderItemResponseDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponseDto> items) {
        this.items = items;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public OrderStatus getEstado() {
        return estado;
    }

    public void setEstado(OrderStatus estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
