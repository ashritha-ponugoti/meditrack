package com.meditrack.service;

import com.meditrack.config.JwtUtil;
import com.meditrack.dto.AuthResponse;
import com.meditrack.dto.LoginRequest;
import com.meditrack.dto.RegisterRequest;
import com.meditrack.model.Pharmacy;
import com.meditrack.model.Role;
import com.meditrack.model.User;
import com.meditrack.repository.PharmacyRepository;
import com.meditrack.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       PharmacyRepository pharmacyRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        Long pharmacyId = user.getPharmacy() != null ? user.getPharmacy().getId() : null;
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), pharmacyId);
    }

    /**
     * Self-service sign-up. Creates a NEW pharmacy and makes the new user its OWNER, so a new
     * account only ever has access to its own pharmacy's data. No email is collected on purpose:
     * alert emails go to a stored address, and a public form must not let anyone point them
     * at someone else's inbox. The password is stored only as a BCrypt hash.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim().toLowerCase();
        if (!username.matches("[a-z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must be 3 to 30 characters: letters, numbers or underscore only.");
        }

        String password = request.getPassword();
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new IllegalArgumentException("Password must be between 8 and 64 characters.");
        }

        String pharmacyName = request.getPharmacyName() == null ? "" : request.getPharmacyName().trim();
        if (pharmacyName.length() < 2 || pharmacyName.length() > 60) {
            throw new IllegalArgumentException("Pharmacy name must be between 2 and 60 characters.");
        }

        String city = request.getCity() == null ? "" : request.getCity().trim();
        if (city.length() > 40) {
            throw new IllegalArgumentException("City must be 40 characters or fewer.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalStateException("That username is already taken. Please choose another.");
        }

        // No coordinates for a self-registered pharmacy, so it is simply never matched for transfers.
        Pharmacy pharmacy = pharmacyRepository.save(
                new Pharmacy(pharmacyName, city.isEmpty() ? null : city, null, null));
        User user = userRepository.save(
                new User(username, passwordEncoder.encode(password), Role.OWNER, pharmacy));

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name(), pharmacy.getId());
    }
}
