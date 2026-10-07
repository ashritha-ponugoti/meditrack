package com.meditrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String manufacturer;

    // Running baseline used for anomaly detection - updated after each
    // transaction via a simple incremental mean/variance update.
    private Double avgDispenseQty = 0.0;
    private Double dispenseQtyStdDev = 0.0;
    private Integer transactionCount = 0;

    public Medicine(String name, String manufacturer) {
        this.name = name;
        this.manufacturer = manufacturer;
    }
}
