package com.shelfsync.dtos;

import com.shelfsync.models.Item;
import com.shelfsync.models.Warehouse;

public record WarehouseItemResponse(Warehouse warehouse, Item item, Integer quantity) {

}
