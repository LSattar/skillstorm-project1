package com.skillstorm.models;

import java.time.OffsetDateTime;

import jakarta.persistence.*;


@Entity
@Table(name = "inventory_history")
public class InventoryHistory {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int inventoryHistoryId;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;

    @ManyToOne
    @JoinColumn(name = "to_warehouse_id")
    private Warehouse toWarehouse;

    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @ManyToOne
    @JoinColumn(name = "performed_by_employee_id")
    private Employee performedBy;

    @PrePersist
    public void onCreate() {
        if (occurredAt == null) {
            occurredAt = OffsetDateTime.now();
        }
    }
    
    public InventoryHistory() {}

	public InventoryHistory(int inventoryHistoryId, Item item, Warehouse fromWarehouse, Warehouse toWarehouse,
			Integer quantityChange, String transactionType, String reason, OffsetDateTime occurredAt,
			Employee performedBy) {
		super();
		this.inventoryHistoryId = inventoryHistoryId;
		this.item = item;
		this.fromWarehouse = fromWarehouse;
		this.toWarehouse = toWarehouse;
		this.quantityChange = quantityChange;
		this.transactionType = transactionType;
		this.reason = reason;
		this.occurredAt = occurredAt;
		this.performedBy = performedBy;
	}

	public int getInventoryHistoryId() {
		return inventoryHistoryId;
	}

	public void setInventoryHistoryId(int inventoryHistoryId) {
		this.inventoryHistoryId = inventoryHistoryId;
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	public Warehouse getFromWarehouse() {
		return fromWarehouse;
	}

	public void setFromWarehouse(Warehouse fromWarehouse) {
		this.fromWarehouse = fromWarehouse;
	}

	public Warehouse getToWarehouse() {
		return toWarehouse;
	}

	public void setToWarehouse(Warehouse toWarehouse) {
		this.toWarehouse = toWarehouse;
	}

	public Integer getQuantityChange() {
		return quantityChange;
	}

	public void setQuantityChange(Integer quantityChange) {
		this.quantityChange = quantityChange;
	}

	public String getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public OffsetDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(OffsetDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public Employee getPerformedBy() {
		return performedBy;
	}

	public void setPerformedBy(Employee performedBy) {
		this.performedBy = performedBy;
	}
    	
}
