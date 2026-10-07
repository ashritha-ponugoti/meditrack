package com.meditrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispensing_transactions")
@Getter
@Setter
@NoArgsConstructor
public class DispensingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(optional = false)
    @JoinColumn(name = "pharmacy_id")
    private Pharmacy pharmacy;

    @ManyToOne
    @JoinColumn(name = "dispensed_by")
    private User dispensedBy;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    private Boolean anomalyFlagged = false;
    private Double anomalyScore;

    public DispensingTransaction(Batch batch, Medicine medicine, Pharmacy pharmacy,
                                  User dispensedBy, Integer quantity) {
        this.batch = batch;
        this.medicine = medicine;
        this.pharmacy = pharmacy;
        this.dispensedBy = dispensedBy;
        this.quantity = quantity;
    }
}
