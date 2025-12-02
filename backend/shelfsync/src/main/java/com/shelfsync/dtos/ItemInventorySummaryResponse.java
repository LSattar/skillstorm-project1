package com.shelfsync.dtos;

import java.util.List;

public record ItemInventorySummaryResponse(Integer itemId, String sku, String gameTitle, Integer totalQuantity,
		List<ItemWarehouseQuantity> locations) {
}