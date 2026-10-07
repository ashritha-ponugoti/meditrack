package com.meditrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @Column(nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDate manufactureDate;

    @Column(nullable = false)
    private LocalDate expiryDate;

    private String supplier;

    public Batch(Medicine medicine, Pharmacy pharmacy, String batchNumber, Integer quantity,
                 LocalDate manufactureDate, LocalDate expiryDate, String supplier) {
        this.medicine = medicine;
        this.pharmacy = pharmacy;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.supplier = supplier;
    }

    public long getDaysToExpiry() {
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }
}
