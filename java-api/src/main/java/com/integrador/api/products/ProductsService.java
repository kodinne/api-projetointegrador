package com.integrador.api.products;

import com.integrador.api.products.dto.CreateProductRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Map;

@Service
public class ProductsService {
    private final ProductRepository productRepository;

    public ProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product create(CreateProductRequest request) {
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setCategory(request.category());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setStatus("active");
        return productRepository.save(product);
    }

    public Map<String, Object> findAll(int page, int limit, String status, String q) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        Specification<Product> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("sku"), like),
                        cb.like(root.get("category"), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var result = productRepository.findAll(
                spec,
                PageRequest.of(safePage - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return Map.of(
                "items", result.getContent(),
                "total", result.getTotalElements(),
                "page", safePage,
                "limit", safeLimit
        );
    }

    public Product findOne(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto com id " + id + " nao encontrado"));
    }

    public Product updateStock(Long id, Integer stock) {
        Product product = findOne(id);
        product.setStock(stock);
        return productRepository.save(product);
    }

    public void remove(Long id) {
        Product product = findOne(id);
        productRepository.delete(product);
    }
}
