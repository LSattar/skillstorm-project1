package com.shelfsync.models;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouse_item")
public class WarehouseItem {

	@EmbeddedId
    private WarehouseItemKey id;

    @ManyToOne
    @MapsId("warehouseId")
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private Integer quantity;
    
    public WarehouseItem() {}

	public WarehouseItem(WarehouseItemKey id, Warehouse warehouse, Item item, Integer quantity) {
		super();
		this.id = id;
		this.warehouse = warehouse;
		this.item = item;
		this.quantity = quantity;
	}

	public WarehouseItemKey getId() {
		return id;
	}

	public void setId(WarehouseItemKey id) {
		this.id = id;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
    
}
