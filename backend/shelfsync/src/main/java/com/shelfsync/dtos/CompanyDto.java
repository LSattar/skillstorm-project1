package com.shelfsync.dtos;

import jakarta.validation.constraints.NotBlank;

public record CompanyDto(Integer id, @NotBlank(message= "company name is required")String name, String phone, String email, String contactPerson) {
}
