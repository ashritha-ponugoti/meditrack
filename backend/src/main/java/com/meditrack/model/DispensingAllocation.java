package com.meditrack.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per batch used by a dispensing transaction. A single dispense can
 * draw from several batches (FEFO), so this table is the accurate record of
 * which batch gave how many units and what was left in it afterwards.
 */
@Entity
@Table(name = "dispensing_allocations")
@Getter
@Setter
@NoArgsConstructor
public class DispensingAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private DispensingTransaction transaction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    // Copied at dispense time so the history stays readable even if the batch changes later.
    @Column(nullable = false)
    private String batchNumber;

    @Column(nullable = false)
    private Integer quantityTaken;

    @Column(nullable = false)
    private Integer remainingAfter;

    public DispensingAllocation(DispensingTransaction transaction, Batch batch,
                                int quantityTaken, int remainingAfter) {
        this.transaction = transaction;
        this.batch = batch;
        this.batchNumber = batch.getBatchNumber();
        this.quantityTaken = quantityTaken;
        this.remainingAfter = remainingAfter;
    }
}
