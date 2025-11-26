package com.shelfsync.models;

import jakarta.persistence.*;

@Entity
@Table(name = "location")
public class Location {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int locationId;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private String aisle;
    private String rack;
    
    public Location() {}
    
	public Location(int locationId, Warehouse warehouse, String aisle, String rack) {
		super();
		this.locationId = locationId;
		this.warehouse = warehouse;
		this.aisle = aisle;
		this.rack = rack;
	}

	public int getLocationId() {
		return locationId;
	}

	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	public Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public String getAisle() {
		return aisle;
	}

	public void setAisle(String aisle) {
		this.aisle = aisle;
	}

	public String getRack() {
		return rack;
	}

	public void setRack(String rack) {
		this.rack = rack;
	}
	
}
