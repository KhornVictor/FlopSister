package edu.itc.shop.web;

import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.itc.shop.domain.Role;
import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.service.AccountService;
import edu.itc.shop.web.dto.RegisterRequest;
import jakarta.validation.Valid;

/**
 * Handles public user registration.
 */
@RestController
@RequestMapping("/api/register")
public class RegisterController {

    private final AccountService accountService;

    public RegisterController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserAccount account = accountService.register(
                request.username(),
                request.password(),
                Set.of(Role.CUSTOMER)
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("username", account.getUsername()));
    }
}
