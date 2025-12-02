package com.shelfsync.dtos;

import java.math.BigDecimal;

public record WarehouseCapacityResponse(
        Integer warehouseId,
        BigDecimal maximumCapacityCubicFeet,
        BigDecimal usedCapacityCubicFeet,
        BigDecimal availableCapacityCubicFeet,
        BigDecimal utilizationPercent
) {}
