package com.fairshare.debt_settlement.dto;

import lombok.Data;

@Data
public class CreatePersonRequest {
    private String name;
    private String email;
    private String phoneNumber;
}
