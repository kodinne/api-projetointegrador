package com.integrador.api.customers;

import com.integrador.api.orders.Order;
import com.integrador.api.orders.OrderRepository;
import com.integrador.api.users.User;
import com.integrador.api.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Test
    void deleteShouldFailWhenCustomerHasLinkedOrders() {
        CustomersService service = new CustomersService(userRepository, orderRepository);

        User customer = new User();
        customer.setId(10L);
        customer.setName("Cliente Teste");
        customer.setEmail("cliente@teste.com");
        customer.setPasswordHash(BCrypt.hashpw("cliente123", BCrypt.gensalt(10)));

        Order linkedOrder = new Order();
        linkedOrder.setCustomer(customer);

        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(orderRepository.findAll()).thenReturn(List.of(linkedOrder));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.delete(10L));
        assertEquals(
                "Cliente possui vendas vinculadas e nao pode ser removido. Apague as vendas primeiro.",
                ex.getMessage()
        );
    }
}
