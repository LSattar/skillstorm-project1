package com.shelfsync.dtos;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryHistoryDto(Integer id, Integer itemId, Integer fromWarehouseId, Integer toWarehouseId,
		Integer quantityChange, String transactionType, String reason, OffsetDateTime occurredAt,
		UUID performedByEmployeeId) {
}
