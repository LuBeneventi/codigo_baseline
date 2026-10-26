package com.pedidos360.catalog.controller;

import com.pedidos360.catalog.dto.ErrorResponseDto;
import com.pedidos360.catalog.dto.ProductCreateDto;
import com.pedidos360.catalog.dto.ProductResponseDto;
import com.pedidos360.catalog.dto.ProductUpdateDto;
import com.pedidos360.catalog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/products")
@Tag(name = "Product Catalog", description = "Endpoints para el CRUD de productos y control de stock de Pedidos360")
public class ProductController {

    private final ProductService productService;
    private final org.springframework.context.ApplicationContext applicationContext;

    public ProductController(ProductService productService, org.springframework.context.ApplicationContext applicationContext) {
        this.productService = productService;
        this.applicationContext = applicationContext;
    }

    // BASELINE-VULN [SEC-REQ-010]: consulta con texto concatenado, sin parametrizar
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam(value = "q", required = false, defaultValue = "") String q) {
        String sql = "SELECT * FROM products WHERE name ILIKE '%" + (q != null ? q : "") + "%'";
        jakarta.persistence.EntityManager entityManager = applicationContext.getBean(jakarta.persistence.EntityManager.class);
        List<?> results = entityManager.createNativeQuery(sql, com.pedidos360.catalog.entity.Product.class).getResultList();
        return ResponseEntity.ok(results);
    }

    @GetMapping
    @Operation(summary = "Listar todos los productos del catálogo",
               description = "Devuelve el listado completo de productos registrados. Consulta abierta para consumo del BFF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de productos obtenido exitosamente",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = ProductResponseDto.class))))
    })
    public ResponseEntity<List<ProductResponseDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo producto",
               description = "Da de alta un producto en el catálogo con nombre, precio, stock e imagen opcional.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o stock negativo",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody ProductCreateDto createDto) {
        ProductResponseDto created = productService.createProduct(createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar precio, stock y/o imagen de un producto",
               description = "Permite modificar el precio, el stock, o imagen de forma independiente o conjunta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado exitosamente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida o intento de fijar stock negativo",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ProductResponseDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateDto updateDto) {
        ProductResponseDto updated = productService.updateProduct(id, updateDto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar producto por su ID",
               description = "Obtiene los detalles del producto especificado por su identificador.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<ProductResponseDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un producto del catálogo",
               description = "Elimina permanentemente el producto del catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
