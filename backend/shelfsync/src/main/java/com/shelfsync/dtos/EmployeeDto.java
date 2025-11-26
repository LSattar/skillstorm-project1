package com.shelfsync.dtos;

import java.util.UUID;

public record EmployeeDto(UUID id, String passwordHash, String firstName, String lastName, String phone, String email,
		Integer assignedWarehouseId) {
}
