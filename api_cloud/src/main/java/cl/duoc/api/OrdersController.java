package cl.duoc.api;

import cl.duoc.api.dto.EnrichedOrderItemDto;
import cl.duoc.api.dto.EnrichedOrderResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class OrdersController {

    private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

    private final RestClient ordersServiceClient;
    private final RestClient catalogServiceClient;
    private final ObjectMapper objectMapper;

    public OrdersController(
            @Qualifier("ordersServiceClient") RestClient ordersServiceClient,
            @Qualifier("catalogServiceClient") RestClient catalogServiceClient,
            ObjectMapper objectMapper) {
        this.ordersServiceClient = ordersServiceClient;
        this.catalogServiceClient = catalogServiceClient;
        this.objectMapper = objectMapper;
    }

    // ==========================================
    // ENDPOINTS DE CATÁLOGO PROXY EN EL BFF
    // ==========================================

    @GetMapping("/api/catalog/products")
    public String getAllProducts() {
        return catalogServiceClient
            .get()
            .uri("/api/catalog/products")
            .retrieve()
            .body(String.class);
    }

    @GetMapping("/api/catalog/products/{id}")
    public String getProductById(@PathVariable String id) {
        return catalogServiceClient
            .get()
            .uri("/api/catalog/products/" + id)
            .retrieve()
            .body(String.class);
    }

    @GetMapping("/api/catalog/products/search")
    public String searchProducts(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String targetUri = "/api/catalog/products/search" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
        return catalogServiceClient
            .get()
            .uri(targetUri)
            .retrieve()
            .body(String.class);
    }

    @PostMapping(value = "/api/catalog/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createProduct(@RequestBody String body) {
        return catalogServiceClient
            .post()
            .uri("/api/catalog/products")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(String.class);
    }

    // ==========================================
    // ENDPOINTS DE ÓRDENES EN EL BFF
    // ==========================================

    // BASELINE-VULN [SEC-REQ-004]: no se comprueba dueno de las ordenes al listar (BOLA/IDOR)
    @GetMapping("/api/orders")
    public String orders(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String targetUri = "/api/orders" + (queryString != null && !queryString.isBlank() ? "?" + queryString : "");
        return ordersServiceClient
            .get()
            .uri(targetUri)
            .retrieve()
            .body(String.class);
    }

    // BASELINE-VULN [SEC-REQ-004]: no se comprueba dueno de la orden por ID (BOLA/IDOR)
    @GetMapping("/api/orders/{id}")
    public String getOrderById(@PathVariable String id) {
        return ordersServiceClient
            .get()
            .uri("/api/orders/" + id)
            .retrieve()
            .body(String.class);
    }

    /**
     * Crear orden a través del BFF:
     * El cliente envía solo { "productoId": 1, "cantidad": 2 }.
     * El BFF consulta el Catálogo para obtener el nombre y precio real vigente,
     * ensambla el payload seguro y lo envía al microservicio de Órdenes.
     */
    // BASELINE-VULN [SEC-REQ-006]: solo completa descripcion y precioUnitario si no vienen; si vienen respeta los del cliente
    @PostMapping(value = "/api/orders", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrder(@RequestBody String body) {
        try {
            JsonNode rootNode = objectMapper.readTree(body);

            // Si el payload contiene items, resolvemos descripcion y precio desde catalogo
            if (rootNode.has("items") && rootNode.get("items").isArray()) {
                ObjectNode orderPayload = rootNode.deepCopy();
                ArrayNode resolvedItems = objectMapper.createArrayNode();

                for (JsonNode itemNode : rootNode.get("items")) {
                    ObjectNode itemCopy = itemNode.deepCopy();

                    Long productoId = itemNode.hasNonNull("productoId") ? itemNode.get("productoId").asLong() : null;

                    // Si no trae descripcion o precioUnitario pero sí tiene productoId, consultamos catálogo
                    if (productoId != null && (!itemNode.hasNonNull("descripcion") || !itemNode.hasNonNull("precioUnitario"))) {
                        try {
                            String productRaw = catalogServiceClient
                                    .get()
                                    .uri("/api/catalog/products/" + productoId)
                                    .retrieve()
                                    .body(String.class);

                            if (productRaw != null) {
                                JsonNode productNode = objectMapper.readTree(productRaw);
                                if (!itemNode.hasNonNull("descripcion") && productNode.hasNonNull("name")) {
                                    itemCopy.put("descripcion", productNode.get("name").asText());
                                }
                                if (!itemNode.hasNonNull("precioUnitario") && productNode.hasNonNull("price")) {
                                    itemCopy.put("precioUnitario", productNode.get("price").asDouble());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error al consultar el catálogo para el producto {}: {}", productoId, e.getMessage());
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body(Map.of("error", "El producto con ID " + productoId + " no existe en el catálogo o no está disponible"));
                        }
                    }

                    resolvedItems.add(itemCopy);
                }

                orderPayload.set("items", resolvedItems);
                String resolvedBody = objectMapper.writeValueAsString(orderPayload);

                return ordersServiceClient
                        .post()
                        .uri("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(resolvedBody)
                        .retrieve()
                        .toEntity(String.class);
            }

            // Fallback si no tiene array de items
            return ordersServiceClient
                    .post()
                    .uri("/api/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

        } catch (Exception e) {
            log.error("Error al procesar la creación de la orden en el BFF: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error procesando solicitud de orden: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/api/orders/{id}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> updateOrderStatus(@PathVariable String id, @RequestBody String body) {
        return ordersServiceClient
            .put()
            .uri("/api/orders/" + id + "/status")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toEntity(String.class);
    }

    // ==========================================
    // ENDPOINT BFF ENRIQUECIDO (AGREGADOR)
    // ==========================================

    /**
     * Endpoint BFF enriquecido: obtiene la orden desde el microservicio de órdenes
     * y para cada ítem que posea productoId consulta el catálogo para resolver
     * nombre, descripción, stock, imagen, etc.
     */
    // BASELINE-VULN [SEC-REQ-004]: endpoint enriquecido no comprueba dueno de la orden
    @GetMapping("/api/orders/{id}/enriched")
    public ResponseEntity<?> getEnrichedOrderById(@PathVariable String id) {
        try {
            String rawOrder = ordersServiceClient
                    .get()
                    .uri("/api/orders/" + id)
                    .retrieve()
                    .body(String.class);

            if (rawOrder == null) {
                return ResponseEntity.notFound().build();
            }

            JsonNode orderNode = objectMapper.readTree(rawOrder);
            Long orderId = orderNode.hasNonNull("id") ? orderNode.get("id").asLong() : null;
            String clienteId = orderNode.hasNonNull("clienteId") ? orderNode.get("clienteId").asText() : null;
            String clienteNombre = orderNode.hasNonNull("clienteNombre") ? orderNode.get("clienteNombre").asText() : null;
            String clienteEmail = orderNode.hasNonNull("clienteEmail") ? orderNode.get("clienteEmail").asText() : null;
            String clienteDireccion = orderNode.hasNonNull("clienteDireccion") ? orderNode.get("clienteDireccion").asText() : null;
            String clienteRut = orderNode.hasNonNull("clienteRut") ? orderNode.get("clienteRut").asText() : null;
            BigDecimal total = orderNode.hasNonNull("total") ? new BigDecimal(orderNode.get("total").asText()) : null;
            String estado = orderNode.hasNonNull("estado") ? orderNode.get("estado").asText() : null;
            String fechaCreacion = orderNode.hasNonNull("fechaCreacion") ? orderNode.get("fechaCreacion").asText() : null;
            String fechaActualizacion = orderNode.hasNonNull("fechaActualizacion") ? orderNode.get("fechaActualizacion").asText() : null;

            List<EnrichedOrderItemDto> enrichedItems = new ArrayList<>();
            if (orderNode.has("items") && orderNode.get("items").isArray()) {
                for (JsonNode itemNode : orderNode.get("items")) {
                    Long itemId = itemNode.hasNonNull("id") ? itemNode.get("id").asLong() : null;
                    Long productoId = itemNode.hasNonNull("productoId") ? itemNode.get("productoId").asLong() : null;
                    String descripcion = itemNode.hasNonNull("descripcion") ? itemNode.get("descripcion").asText() : null;
                    Integer cantidad = itemNode.hasNonNull("cantidad") ? itemNode.get("cantidad").asInt() : null;
                    BigDecimal precioUnitario = itemNode.hasNonNull("precioUnitario") ? new BigDecimal(itemNode.get("precioUnitario").asText()) : null;
                    BigDecimal subtotal = itemNode.hasNonNull("subtotal") ? new BigDecimal(itemNode.get("subtotal").asText()) : null;

                    Map<String, Object> productDetail = null;
                    if (productoId != null) {
                        try {
                            String productRaw = catalogServiceClient
                                    .get()
                                    .uri("/api/catalog/products/" + productoId)
                                    .retrieve()
                                    .body(String.class);
                            if (productRaw != null) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = objectMapper.readValue(productRaw, Map.class);
                                productDetail = map;
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo obtener el detalle del producto {} del catálogo: {}", productoId, e.getMessage());
                        }
                    }

                    enrichedItems.add(new EnrichedOrderItemDto(
                            itemId,
                            productoId,
                            descripcion,
                            cantidad,
                            precioUnitario,
                            subtotal,
                            productDetail
                    ));
                }
            }

            // BASELINE-VULN [SEC-REQ-007]: devuelve nombre, correo, rut y direccion del cliente a cualquiera
            EnrichedOrderResponseDto enrichedResponse = new EnrichedOrderResponseDto(
                    orderId,
                    clienteId,
                    clienteNombre,
                    clienteEmail,
                    clienteDireccion,
                    clienteRut,
                    enrichedItems,
                    total,
                    estado,
                    fechaCreacion,
                    fechaActualizacion
            );

            return ResponseEntity.ok(enrichedResponse);
        } catch (Exception e) {
            log.error("Error al procesar orden enriquecida con id {}: {}", id, e.getMessage());
            // BASELINE-VULN [SEC-REQ-014]: controlador devuelve e.getMessage() revelando detalles de excepcion interna
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}