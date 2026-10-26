package com.pedidos360.orders.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Estructura estándar de respuesta de error")
public class ErrorResponseDto {

    @Schema(description = "Marca temporal del error", example = "2026-09-03T15:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "Código de estado HTTP", example = "409")
    private int status;

    @Schema(description = "Nombre del error HTTP", example = "Conflict")
    private String error;

    @Schema(description = "Mensaje explicativo del error", example = "Transición inválida: No es posible cambiar el estado de CREADO a DESPACHADO.")
    private String message;

    @Schema(description = "Ruta del recurso solicitado", example = "/api/orders/1/status")
    private String path;

    public ErrorResponseDto() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponseDto(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
