package com.meditrack.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispenseRequest {
    private String medicineName;
    private Integer quantity;
}
