package edu.itc.shop.service;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Role;
import edu.itc.shop.domain.UserAccount;
import edu.itc.shop.repository.UserAccountRepository;

/**
 * Handles user account registration.
 */
@Service
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount register(String username, String rawPassword, Set<Role> roles) {

        if (userAccountRepository.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        UserAccount account = new UserAccount(
                username,
                passwordHash,
                true,
                roles
        );

        return userAccountRepository.save(account);
    }

    /**
     * Checks whether an account already exists.
     */
    public boolean existsByUsername(String username) {
        return userAccountRepository.existsByUsername(username);
    }
}
