package cl.duoc.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public record EnrichedOrderItemDto(
        Long id,
        Long productoId,
        String descripcion,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        Map<String, Object> productoDetalle
) {}
