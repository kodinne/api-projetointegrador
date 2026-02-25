package com.integrador.api.products;

import com.integrador.api.orders.OrderItemRepository;
import com.integrador.api.products.dto.CreateProductRequest;
import com.integrador.api.returns.ReturnRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ReturnRecordRepository returnRecordRepository;

    @Test
    void createShouldFailWhenSkuAlreadyExists() {
        ProductsService service = new ProductsService(productRepository, orderItemRepository, returnRecordRepository);
        CreateProductRequest request = new CreateProductRequest(
                "SKU-001",
                "Produto X",
                "Categoria",
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                10
        );

        when(productRepository.existsBySkuIgnoreCase("SKU-001")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(request));
        assertEquals(HttpStatus.CONFLICT.value(), ex.getStatusCode().value());
    }

    @Test
    void updateStockShouldFailWhenStockIsNegative() {
        ProductsService service = new ProductsService(productRepository, orderItemRepository, returnRecordRepository);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.updateStock(1L, -1));
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
    }
}
