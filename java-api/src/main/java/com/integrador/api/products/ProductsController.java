package com.integrador.api.products;

import com.integrador.api.products.dto.CreateProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductsController {
    private final ProductsService productsService;

    public ProductsController(ProductsService productsService) {
        this.productsService = productsService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(productsService.create(request));
    }

    @GetMapping
    public ResponseEntity<?> findAll(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q
    ) {
        return ResponseEntity.ok(productsService.findAll(page, limit, status, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id) {
        return ResponseEntity.ok(productsService.findOne(id));
    }

    @PatchMapping("/{id}/stock/{stock}")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @PathVariable Integer stock) {
        return ResponseEntity.ok(productsService.updateStock(id, stock));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable Long id) {
        return ResponseEntity.ok(productsService.remove(id));
    }
}
