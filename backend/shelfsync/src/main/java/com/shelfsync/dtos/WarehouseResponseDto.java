package com.shelfsync.dtos;

import java.math.BigDecimal;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shelfsync.models.Employee;

public record WarehouseResponseDto(Integer id, String name, String address, String city, String state, String zip,
		@JsonIgnoreProperties({"assignedWarehouse"}) Employee manager, BigDecimal maximumCapacityCubicFeet) {

}
