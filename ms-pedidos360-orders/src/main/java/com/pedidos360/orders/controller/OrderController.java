package com.pedidos360.orders.controller;

import com.pedidos360.orders.dto.ErrorResponseDto;
import com.pedidos360.orders.dto.OrderRequestDto;
import com.pedidos360.orders.dto.OrderResponseDto;
import com.pedidos360.orders.dto.UpdateStatusRequestDto;
import com.pedidos360.orders.entity.OrderStatus;
import com.pedidos360.orders.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Operaciones CRUD y ciclo de vida de pedidos")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo pedido", description = "Registra un pedido en estado inicial CREADO calculando el total a partir de sus ítems.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado exitosamente",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<OrderResponseDto> createOrder(@Valid @RequestBody OrderRequestDto request) {
        OrderResponseDto created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un pedido por ID", description = "Obtiene la información completa de un pedido específico a partir de su ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<OrderResponseDto> getOrderById(
            @Parameter(description = "Identificador único del pedido", example = "1")
            @PathVariable Long id) {
        OrderResponseDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Actualizar el estado de un pedido", description = "Avanza o cambia el estado respetando la máquina de estados. Retorna 409 Conflict si la transición no es válida.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Transición de estado no permitida",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @Parameter(description = "Identificador único del pedido", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequestDto request) {
        OrderResponseDto updated = orderService.updateOrderStatus(id, request.getNuevoEstado());
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    @Operation(summary = "Listar pedidos con filtros opcionales", description = "Lista pedidos filtrando opcionalmente por estado y rango de fechas de creación.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de pedidos",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OrderResponseDto.class)))),
            @ApiResponse(responseCode = "400", description = "Parámetros de consulta inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<List<OrderResponseDto>> listOrders(
            @Parameter(description = "Filtrar por estado del pedido", example = "CREADO")
            @RequestParam(required = false) OrderStatus status,
            @Parameter(description = "Fecha inicial del rango de creación (ISO-8601)", example = "2026-09-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha final del rango de creación (ISO-8601)", example = "2026-09-30T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        List<OrderResponseDto> orders = orderService.listOrders(status, from, to);
        return ResponseEntity.ok(orders);
    }
}
