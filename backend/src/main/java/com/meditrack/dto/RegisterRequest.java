package com.meditrack.dto;

import lombok.Getter;
import lombok.Setter;

/** Sign-up form: creates a new pharmacy together with its owner account. */
@Getter
@Setter
public class RegisterRequest {
    private String username;
    private String password;
    private String pharmacyName;
    private String city;
}
