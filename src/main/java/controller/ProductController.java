package saimart.controller;

import saimart.entity.Product;
import saimart.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // GET ALL PRODUCTS (with optional search and category filters)
    @GetMapping
    public List<Product> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {

        List<Product> products = productRepository.findAll();

        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
            String catLower = category.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> p.getCategory() != null && p.getCategory().toLowerCase().contains(catLower))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> (p.getName() != null && p.getName().toLowerCase().contains(q))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q))
                            || (p.getCategory() != null && p.getCategory().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        return products;
    }

    @jakarta.annotation.PostConstruct
    public void initProducts() {
        try {
            List<Product> all = productRepository.findAll();
            boolean changed = false;
            for (Product p : all) {
                if (p.getSellerId() == null) {
                    p.setSellerId(1L);
                    changed = true;
                }
            }
            if (changed) {
                productRepository.saveAll(all);
            }
        } catch (Exception ignored) {
        }
    }

    // GET PRODUCTS OF ONE SELLER
    @GetMapping("/seller/{sellerId}")
    public List<Product> getSellerProducts(@PathVariable Long sellerId) {
        List<Product> products = productRepository.findBySellerId(sellerId);
        // Fallback: If no products found or for default seller (ID 1), include unassigned products
        if (products.isEmpty() || Long.valueOf(1L).equals(sellerId)) {
            List<Product> all = productRepository.findAll();
            List<Product> matched = all.stream()
                    .filter(p -> p.getSellerId() == null || p.getSellerId().equals(sellerId))
                    .collect(Collectors.toList());
            if (!matched.isEmpty()) {
                return matched;
            }
        }
        return products;
    }

    // ADD PRODUCT
    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product name is required."));
        }
        if (product.getPrice() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product price must be greater than zero."));
        }
        if (product.getStock() < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock cannot be negative."));
        }
        if (product.getCategory() == null || product.getCategory().trim().isEmpty()) {
            product.setCategory("Electronics");
        }
        if (product.getSellerId() == null || product.getSellerId() <= 0) {
            product.setSellerId(1L);
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // GET SINGLE PRODUCT
    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found with ID: " + id));
        }
        return ResponseEntity.ok(productOpt.get());
    }

    // UPDATE PRODUCT
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct) {

        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found."));
        }

        if (updatedProduct.getName() == null || updatedProduct.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product name cannot be empty."));
        }
        if (updatedProduct.getPrice() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Price must be greater than zero."));
        }
        if (updatedProduct.getStock() < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Stock cannot be negative."));
        }

        Product product = productOpt.get();
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setCategory(updatedProduct.getCategory() != null ? updatedProduct.getCategory() : product.getCategory());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setImageUrl(updatedProduct.getImageUrl());
        if (updatedProduct.getSellerId() != null && updatedProduct.getSellerId() > 0) {
            product.setSellerId(updatedProduct.getSellerId());
        } else if (product.getSellerId() == null) {
            product.setSellerId(1L);
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.ok(saved);
    }

    // DELETE PRODUCT
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found."));
        }

        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    // SEED DEMO PRODUCTS (Utility for clean presentation)
    @PostMapping("/seed")
    public ResponseEntity<?> seedDemoProducts() {
        if (productRepository.count() >= 8) {
            return ResponseEntity.ok(Map.of("message", "Products already seeded", "count", productRepository.count()));
        }

        List<Product> demoProducts = List.of(
            createProduct("Sony WH-1000XM5 Noise Cancelling Headphones", "Industry leading active noise cancellation, 30-hour battery life, and ultra-comfortable design.", "Electronics", 24999.0, 15, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600", 1L),
            createProduct("Apple Watch Series 9 GPS 45mm", "Advanced health sensors, bright Always-On display, Crash Detection, and fast charging.", "Electronics", 41900.0, 10, "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600", 1L),
            createProduct("Nike Air Jordan Retro High OG", "Classic high-top basketball sneakers featuring genuine premium leather and responsive Air cushioning.", "Fashion", 13995.0, 18, "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600", 1L),
            createProduct("Bellroy Classic Backpack Plus 24L", "Durable water-resistant recycled fabric with dedicated padded 16 inch laptop sleeve.", "Fashion", 6499.0, 25, "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600", 1L),
            createProduct("Sony PlayStation 5 DualSense Controller", "Immersive haptic feedback, dynamic adaptive triggers, and built-in microphone.", "Gaming", 5990.0, 22, "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=600", 1L),
            createProduct("Logitech MX Master 3S Wireless Mouse", "Quiet clicks, 8K DPI sensor for any surface tracking, and hyper-fast MagSpeed scrolling.", "Electronics", 8995.0, 30, "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600", 1L),
            createProduct("Philips Hue Smart Ambience LED Lamp", "16 million vibrant colors with warm-to-cool daylight modes and seamless voice control.", "Home", 3499.0, 14, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600", 1L),
            createProduct("Breville Barista Touch Espresso Machine", "Automated touchscreen with pre-programmed cafe drinks, integrated precision grinder.", "Home", 48500.0, 8, "https://images.unsplash.com/photo-1517668808822-9ebb02f2a0e6?w=600", 1L)
        );

        productRepository.saveAll(demoProducts);
        return ResponseEntity.ok(Map.of("message", "Demo catalog seeded successfully", "count", productRepository.count()));
    }

    private Product createProduct(String name, String desc, String cat, double price, int stock, String img, Long sellerId) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setCategory(cat);
        p.setPrice(price);
        p.setStock(stock);
        p.setImageUrl(img);
        p.setSellerId(sellerId);
        return p;
    }
}