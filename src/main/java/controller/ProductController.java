package saimart.controller;

import saimart.entity.Product;
import saimart.repository.ProductRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }
    @GetMapping("/{id}")
public Product getProduct(@PathVariable Long id) {
    return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
}
}