package com.shelfsync.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record WarehouseDto(Integer id, String name, String address, String city, String state, String zip,
		UUID managerEmployeeId, BigDecimal maximumCapacityCubicFeet) {
}
