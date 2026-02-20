package com.integrador.api.returns;

import com.integrador.api.returns.dto.CreateReturnRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/returns")
public class ReturnsController {
    private final ReturnsService returnsService;

    public ReturnsController(ReturnsService returnsService) {
        this.returnsService = returnsService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(returnsService.list());
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateReturnRequest request) {
        return ResponseEntity.ok(returnsService.create(request));
    }
}
