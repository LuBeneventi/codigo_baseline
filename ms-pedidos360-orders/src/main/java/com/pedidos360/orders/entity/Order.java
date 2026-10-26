package com.pedidos360.orders.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El identificador del cliente es obligatorio")
    @Column(name = "cliente_id", nullable = false)
    private String clienteId;

    @Column(name = "cliente_email")
    private String clienteEmail;

    @Column(name = "cliente_nombre")
    private String clienteNombre;

    @Column(name = "cliente_direccion")
    private String clienteDireccion;

    @Column(name = "cliente_rut")
    private String clienteRut;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @NotNull(message = "El total del pedido es obligatorio")
    @Positive(message = "El total del pedido debe ser positivo")
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @NotNull(message = "El estado del pedido es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private OrderStatus estado = OrderStatus.CREADO;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    public Order() {
    }

    public Order(String clienteId) {
        this.clienteId = clienteId;
        this.estado = OrderStatus.CREADO;
    }

    public Order(String clienteId, String clienteNombre, String clienteEmail) {
        this.clienteId = clienteId;
        this.clienteNombre = clienteNombre;
        this.clienteEmail = clienteEmail;
        this.estado = OrderStatus.CREADO;
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

    public String getClienteEmail() {
        if (clienteEmail != null && !clienteEmail.isBlank()) {
            return clienteEmail;
        }
        return "cliente." + clienteId + "@pedidos360.com";
    }

    public void setClienteEmail(String clienteEmail) {
        this.clienteEmail = clienteEmail;
    }

    public String getClienteNombre() {
        if (clienteNombre != null && !clienteNombre.isBlank()) {
            return clienteNombre;
        }
        return "Cliente " + clienteId;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
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

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
        if (items != null) {
            items.forEach(item -> item.setOrder(this));
        }
        recalculateTotal();
    }

    public void addItem(OrderItem item) {
        if (item != null) {
            items.add(item);
            item.setOrder(this);
            recalculateTotal();
        }
    }

    public void removeItem(OrderItem item) {
        if (item != null) {
            items.remove(item);
            item.setOrder(null);
            recalculateTotal();
        }
    }

    public BigDecimal getTotal() {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            recalculateTotal();
        }
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void recalculateTotal() {
        if (items == null || items.isEmpty()) {
            this.total = BigDecimal.ZERO;
            return;
        }
        this.total = items.stream()
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
