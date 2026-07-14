package com.fairshare.debt_settlement.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String phone;
    private Boolean hidePhone;
    private Boolean hideEmail;
    private Boolean notificationsEnabled;
}
