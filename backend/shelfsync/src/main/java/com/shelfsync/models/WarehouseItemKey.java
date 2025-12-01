package com.shelfsync.models;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class WarehouseItemKey implements Serializable {

    @Column(name = "warehouse_id")
    private int warehouseId;

    @Column(name = "item_id")
    private int itemId;

    public WarehouseItemKey() {}

    public WarehouseItemKey(int warehouseId, int itemId) {
        this.warehouseId = warehouseId;
        this.itemId = itemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WarehouseItemKey)) return false;
        WarehouseItemKey that = (WarehouseItemKey) o;
        return Objects.equals(warehouseId, that.warehouseId) &&
               Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, itemId);
    }

	public int getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(int warehouseId) {
		this.warehouseId = warehouseId;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

}
