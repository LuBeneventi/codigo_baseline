package com.pedidos360.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Datos para agregar un ítem al pedido")
public class OrderItemRequestDto {

    @Schema(description = "Identificador del producto en el catálogo (opcional)", example = "1")
    private Long productoId;

    @Schema(description = "Descripción del producto o servicio", example = "Café Colombiano Molido 500g")
    @NotBlank(message = "La descripción del ítem es requerida")
    private String descripcion;

    @Schema(description = "Cantidad solicitada (mínimo 1)", example = "2")
    @NotNull(message = "La cantidad es requerida")
    // BASELINE-VULN [SEC-REQ-010]: sin validacion @Min(1) para cantidad
    private Integer cantidad;

    @Schema(description = "Precio unitario (debe ser mayor a 0)", example = "18.50")
    @NotNull(message = "El precio unitario es requerido")
    // BASELINE-VULN [SEC-REQ-010]: sin validacion @Positive para precioUnitario
    private BigDecimal precioUnitario;

    public OrderItemRequestDto() {
    }

    public OrderItemRequestDto(String descripcion, Integer cantidad, BigDecimal precioUnitario) {
        this(null, descripcion, cantidad, precioUnitario);
    }

    public OrderItemRequestDto(Long productoId, String descripcion, Integer cantidad, BigDecimal precioUnitario) {
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}
