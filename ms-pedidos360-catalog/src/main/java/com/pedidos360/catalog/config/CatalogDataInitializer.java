package com.pedidos360.catalog.config;

import com.pedidos360.catalog.entity.Product;
import com.pedidos360.catalog.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CatalogDataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    public CatalogDataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            productRepository.saveAll(List.of(
                new Product(null, "Café Espresso Roast Blend 250g", 
                    "Café de tueste oscuro con notas intensas de chocolate negro, caramelo y cuerpo denso.", 
                    new BigDecimal("7490.00"), 45, "https://images.unsplash.com/photo-1559056199-641a0ac8b55e?w=500"),
                new Product(null, "Café Colombia Huila Especial 500g", 
                    "Origen 100% Arábica con acidez cítrica brillante, fragancia a jazmín y notas a panela dulce.", 
                    new BigDecimal("11990.00"), 30, "https://images.unsplash.com/photo-1587734195503-904fca47e0e9?w=500"),
                new Product(null, "Café Etiopía Yirgacheffe 250g", 
                    "Variedad Heirloom con notas a bergamota, melocotón y té negro.", 
                    new BigDecimal("9290.00"), 20, "https://images.unsplash.com/photo-1610632380989-680fe40816c6?w=500"),
                new Product(null, "Café Brasil Santos Mogiana 1kg", 
                    "Café suave con notas achocolatadas, avellanas tostadas y gran crema.", 
                    new BigDecimal("18990.00"), 50, "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=500"),
                new Product(null, "Café Geisha Panamá Boquete Reserva 250g", 
                    "Edición limitada con notas elegantes a flor de café, jazmín y lima kaffir.", 
                    new BigDecimal("19990.00"), 12, "https://images.unsplash.com/photo-1518832553480-cd0e625ed3e6?w=500"),
                new Product(null, "Café Descafeinado Swiss Water 250g", 
                    "Descafeinado 100% natural mediante proceso de agua. Sabor a nuez, cacao y canela.", 
                    new BigDecimal("8490.00"), 25, "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=500")
            ));
            System.out.println("[+] Catálogo inicializado con éxito en ms-catalog.");
        }
    }
}
