package com.pedidos360.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Detalle del ítem en la respuesta del pedido")
public class OrderItemResponseDto {

    @Schema(description = "Identificador del ítem", example = "1")
    private Long id;

    @Schema(description = "Identificador del producto en el catálogo (si aplica)", example = "1")
    private Long productoId;

    @Schema(description = "Descripción del producto", example = "Café Colombiano Molido 500g")
    private String descripcion;

    @Schema(description = "Cantidad solicitada", example = "2")
    private Integer cantidad;

    @Schema(description = "Precio unitario", example = "18.50")
    private BigDecimal precioUnitario;

    @Schema(description = "Subtotal calculado (cantidad * precioUnitario)", example = "37.00")
    private BigDecimal subtotal;

    public OrderItemResponseDto() {
    }

    public OrderItemResponseDto(Long id, String descripcion, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
        this(id, null, descripcion, cantidad, precioUnitario, subtotal);
    }

    public OrderItemResponseDto(Long id, Long productoId, String descripcion, Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
        this.id = id;
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
