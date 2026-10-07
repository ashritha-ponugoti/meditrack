package com.meditrack.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BatchRequest {
    private String medicineName;
    private String manufacturer;
    private String batchNumber;
    private Integer quantity;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private String supplier;
}
