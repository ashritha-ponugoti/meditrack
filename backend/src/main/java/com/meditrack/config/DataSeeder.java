package com.meditrack.config;

import com.meditrack.model.*;
import com.meditrack.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds demo data on startup (H2 profile only, data is in-memory anyway).
 * Gives you two pharmacies, a couple of users, and a medicine with batches
 * at different expiry dates - enough to demo FEFO dispensing immediately
 * and to show a surplus/shortage pair for the transfer feature.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoOwnerEmail;

    public DataSeeder(PharmacyRepository pharmacyRepository, UserRepository userRepository,
                       MedicineRepository medicineRepository, BatchRepository batchRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${meditrack.demo.owner-email}") String demoOwnerEmail) {
        this.pharmacyRepository = pharmacyRepository;
        this.userRepository = userRepository;
        this.medicineRepository = medicineRepository;
        this.batchRepository = batchRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoOwnerEmail = demoOwnerEmail;
    }

    @Override
    public void run(String... args) {
        if (pharmacyRepository.count() > 0) return; // already seeded

        Pharmacy pharmacyA = pharmacyRepository.save(
                new Pharmacy("City Pharmacy", "Hyderabad", 17.385, 78.4867));
        Pharmacy pharmacyB = pharmacyRepository.save(
                new Pharmacy("Suburb Pharmacy", "Hyderabad", 17.40, 78.50));

        userRepository.save(new User("owner", passwordEncoder.encode("owner123"), Role.OWNER, pharmacyA, demoOwnerEmail));
        userRepository.save(new User("pharmacist", passwordEncoder.encode("pharma123"), Role.PHARMACIST, pharmacyA));
        // Pharmacy B's owner also uses the same demo inbox, so transfer-suggestion
        // emails for BOTH sides of a suggested transfer land where you can see them.
        userRepository.save(new User("owner2", passwordEncoder.encode("owner123"), Role.OWNER, pharmacyB, demoOwnerEmail));

        Medicine paracetamol = medicineRepository.save(new Medicine("Paracetamol 500mg", "ABC Pharma"));
        medicineRepository.save(new Medicine("Amoxicillin 250mg", "XYZ Labs"));

        // Three batches at Pharmacy A, different expiry dates - demonstrates FEFO ordering.
        batchRepository.save(new Batch(paracetamol, pharmacyA, "PCM-A1", 100,
                LocalDate.now().minusMonths(6), LocalDate.now().plusDays(45), "ABC Pharma"));
        batchRepository.save(new Batch(paracetamol, pharmacyA, "PCM-A2", 150,
                LocalDate.now().minusMonths(3), LocalDate.now().plusDays(180), "ABC Pharma"));
        batchRepository.save(new Batch(paracetamol, pharmacyA, "PCM-A3", 200,
                LocalDate.now().minusMonths(1), LocalDate.now().plusDays(365), "ABC Pharma"));

        // Pharmacy B is low on the same medicine - sets up a transfer-matching demo.
        batchRepository.save(new Batch(paracetamol, pharmacyB, "PCM-B1", 10,
                LocalDate.now().minusMonths(2), LocalDate.now().plusDays(300), "ABC Pharma"));

        System.out.println("=== MediTrack demo data seeded ===");
        System.out.println("Login as owner / owner123  or  pharmacist / pharma123");
        System.out.println("Alert emails will be sent to: " + demoOwnerEmail
                + " (set DEMO_OWNER_EMAIL to your real address to receive them)");
    }
}
