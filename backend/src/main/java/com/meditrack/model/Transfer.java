package com.meditrack.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_pharmacy_id")
    private Pharmacy fromPharmacy;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_pharmacy_id")
    private Pharmacy toPharmacy;

    @Column(nullable = false)
    private Integer suggestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.SUGGESTED;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transfer(Medicine medicine, Pharmacy fromPharmacy, Pharmacy toPharmacy, Integer suggestedQuantity) {
        this.medicine = medicine;
        this.fromPharmacy = fromPharmacy;
        this.toPharmacy = toPharmacy;
        this.suggestedQuantity = suggestedQuantity;
    }
}
