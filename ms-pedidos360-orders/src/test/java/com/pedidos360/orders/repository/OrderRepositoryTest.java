package com.pedidos360.orders.repository;

import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderItem;
import com.pedidos360.orders.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Guardar y recuperar pedido con items en cascada")
    void testSaveAndFind() {
        Order order = new Order("CLI-001");
        OrderItem item = new OrderItem("Producto 1", 3, new BigDecimal("10.00"));
        order.addItem(item);

        Order saved = orderRepository.save(order);
        assertNotNull(saved.getId());
        assertEquals(new BigDecimal("30.00"), saved.getTotal());
        assertNotNull(saved.getFechaCreacion());

        Order retrieved = orderRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.getItems().size());
    }

    @Test
    @DisplayName("Buscar pedidos por estado")
    void testFindByEstado() {
        Order order1 = new Order("CLI-001");
        order1.addItem(new OrderItem("P1", 1, new BigDecimal("5.00")));
        order1.setEstado(OrderStatus.CREADO);
        orderRepository.save(order1);

        Order order2 = new Order("CLI-002");
        order2.addItem(new OrderItem("P2", 2, new BigDecimal("8.00")));
        order2.setEstado(OrderStatus.ACEPTADO);
        orderRepository.save(order2);

        List<Order> creados = orderRepository.findByEstado(OrderStatus.CREADO);
        assertFalse(creados.isEmpty());
        assertTrue(creados.stream().allMatch(o -> o.getEstado() == OrderStatus.CREADO));

        List<Order> aceptados = orderRepository.findByEstado(OrderStatus.ACEPTADO);
        assertFalse(aceptados.isEmpty());
        assertTrue(aceptados.stream().allMatch(o -> o.getEstado() == OrderStatus.ACEPTADO));
    }

    @Test
    @DisplayName("Buscar pedidos por rango de fechas")
    void testFindByFechaCreacionBetween() {
        Order order = new Order("CLI-003");
        order.addItem(new OrderItem("P3", 1, new BigDecimal("15.00")));
        orderRepository.save(order);

        LocalDateTime from = LocalDateTime.now().minusHours(1);
        LocalDateTime to = LocalDateTime.now().plusHours(1);

        List<Order> results = orderRepository.findByFechaCreacionBetween(from, to);
        assertFalse(results.isEmpty());
    }
}
