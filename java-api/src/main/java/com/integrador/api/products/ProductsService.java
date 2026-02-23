package com.integrador.api.products;

import com.integrador.api.orders.OrderItemRepository;
import com.integrador.api.products.dto.CreateProductRequest;
import com.integrador.api.returns.ReturnRecordRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Map;

@Service
public class ProductsService {
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnRecordRepository returnRecordRepository;

    public ProductsService(
            ProductRepository productRepository,
            OrderItemRepository orderItemRepository,
            ReturnRecordRepository returnRecordRepository
    ) {
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
        this.returnRecordRepository = returnRecordRepository;
    }

    public Product create(CreateProductRequest request) {
        String normalizedName = request.name() == null ? "" : request.name().trim();
        if (normalizedName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (productRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nao e possivel cadastrar: este produto ja existe.");
        }

        if (request.price() == null || request.price().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be greater than zero");
        }
        if (request.stock() == null || request.stock() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stock must be greater than zero");
        }
        if (request.category() == null || request.category().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category is required");
        }

        Product product = new Product();
        product.setSku(request.sku());
        product.setName(normalizedName);
        product.setCategory(request.category().trim());
        product.setPrice(request.price());
        product.setCostPrice(request.costPrice() == null ? java.math.BigDecimal.ZERO : request.costPrice());
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

    @Transactional
    public Map<String, Object> remove(Long id) {
        Product product = findOne(id);
        long removedOrderItems = orderItemRepository.deleteByProduct_Id(id);
        long removedReturns = returnRecordRepository.deleteByProductId(id);
        productRepository.delete(product);
        productRepository.flush();

        return Map.of(
                "removed", true,
                "message", "Produto removido com sucesso.",
                "removedOrderItems", removedOrderItems,
                "removedReturns", removedReturns
        );
    }
}
