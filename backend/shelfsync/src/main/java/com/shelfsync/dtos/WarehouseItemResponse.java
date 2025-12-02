package com.shelfsync.dtos;

import com.shelfsync.models.Item;
import com.shelfsync.models.Warehouse;

public record WarehouseItemResponse(Integer warehouseId, String warehouseName, String warehouseAddress,
		String warehouseCity, String warehouseState, String warehouseZip, Item item, Integer quantity) {

}
