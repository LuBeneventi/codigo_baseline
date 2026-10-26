package com.pedidos360.orders.repository;

import com.pedidos360.orders.entity.Order;
import com.pedidos360.orders.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findByEstado(OrderStatus estado);

    List<Order> findByFechaCreacionBetween(LocalDateTime from, LocalDateTime to);

    List<Order> findByEstadoAndFechaCreacionBetween(OrderStatus estado, LocalDateTime from, LocalDateTime to);
}
