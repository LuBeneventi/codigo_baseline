package cl.duoc.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record EnrichedOrderResponseDto(
        Long id,
        String clienteId,
        String clienteNombre,
        String clienteEmail,
        String clienteDireccion,
        String clienteRut,
        List<EnrichedOrderItemDto> items,
        BigDecimal total,
        String estado,
        String fechaCreacion,
        String fechaActualizacion
) {}
