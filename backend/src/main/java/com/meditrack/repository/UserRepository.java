package com.meditrack.repository;

import com.meditrack.model.Pharmacy;
import com.meditrack.model.Role;
import com.meditrack.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    // Used by the alert service to find who should receive expiry-risk
    // and transfer-suggestion emails for a given pharmacy.
    List<User> findByPharmacyAndRole(Pharmacy pharmacy, Role role);
}
