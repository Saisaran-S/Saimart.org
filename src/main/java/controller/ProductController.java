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

    // GET ALL PRODUCTS
    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    // GET PRODUCTS OF ONE SELLER
    @GetMapping("/seller/{sellerId}")
    public List<Product> getSellerProducts(
            @PathVariable Long sellerId) {

        return productRepository.findBySellerId(sellerId);
    }

    // ADD PRODUCT
    @PostMapping
    public Product addProduct(
            @RequestBody Product product) {

        return productRepository.save(product);
    }

    // GET SINGLE PRODUCT
    @GetMapping("/{id}")
    public Product getProduct(
            @PathVariable Long id) {

        return productRepository.findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Product not found"
                        )
                );
    }

    // UPDATE PRODUCT
    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable Long id,
            @RequestBody Product updatedProduct) {

        Product product =
                productRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Product not found"
                                )
                        );

        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setCategory(updatedProduct.getCategory());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setImageUrl(updatedProduct.getImageUrl());

        return productRepository.save(product);
    }

    // DELETE PRODUCT
    @DeleteMapping("/{id}")
    public String deleteProduct(
            @PathVariable Long id) {

        if (!productRepository.existsById(id)) {
            throw new RuntimeException(
                    "Product not found"
            );
        }

        productRepository.deleteById(id);

        return "Product deleted successfully";
    }
}