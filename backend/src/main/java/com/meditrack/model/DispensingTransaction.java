package com.meditrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dispensing_transactions")
@Getter
@Setter
@NoArgsConstructor
public class DispensingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The FIRST batch this dispense drew from. Kept only so the existing table layout
    // stays valid; the authoritative per-batch record is `allocations` below.
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

    // Every batch this dispense used, with the quantity taken from each.
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL)
    @OrderBy("id ASC")
    private List<DispensingAllocation> allocations = new ArrayList<>();

    public DispensingTransaction(Batch batch, Medicine medicine, Pharmacy pharmacy,
                                  User dispensedBy, Integer quantity) {
        this.batch = batch;
        this.medicine = medicine;
        this.pharmacy = pharmacy;
        this.dispensedBy = dispensedBy;
        this.quantity = quantity;
    }

    public void addAllocation(DispensingAllocation allocation) {
        allocation.setTransaction(this);
        this.allocations.add(allocation);
    }
}
