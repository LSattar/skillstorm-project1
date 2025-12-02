package com.shelfsync.dtos;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shelfsync.models.Warehouse;

public record EmployeeResponseDto(UUID id, String firstName, String lastName, String phone, String email,
		@JsonIgnoreProperties({"manager"})Warehouse assignedWarehouse) {

}
