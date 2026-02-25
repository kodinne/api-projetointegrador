package com.integrador.api.users;

import com.integrador.api.orders.OrderRepository;
import com.integrador.api.users.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Test
    void createShouldFailWhenPasswordIsBlank() {
        UsersService service = new UsersService(userRepository, orderRepository);
        CreateUserRequest request = new CreateUserRequest("Nome", "email@teste.com", " ");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(request));
        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
    }
}
