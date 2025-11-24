package com.skillstorm.models;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouse")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int warehouseId;

    @Column(nullable = false)
    private String name;

    private String address;
    private String city;
    private String state;
    private String zip;

    // Manager (Employee)
    @ManyToOne
    @JoinColumn(name = "manager_employee_id")
    private Employee manager;

    @Column(nullable = false)
    private BigDecimal maximumCapacityCubicFeet;

    public Warehouse() {}
    
	public Warehouse(int warehouseId, String name, String address, String city, String state, String zip,
			Employee manager, BigDecimal maximumCapacityCubicFeet) {
		super();
		this.warehouseId = warehouseId;
		this.name = name;
		this.address = address;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.manager = manager;
		this.maximumCapacityCubicFeet = maximumCapacityCubicFeet;
	}

	public int getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(int warehouseId) {
		this.warehouseId = warehouseId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public Employee getManager() {
		return manager;
	}

	public void setManager(Employee manager) {
		this.manager = manager;
	}

	public BigDecimal getMaximumCapacityCubicFeet() {
		return maximumCapacityCubicFeet;
	}

	public void setMaximumCapacityCubicFeet(BigDecimal maximumCapacityCubicFeet) {
		this.maximumCapacityCubicFeet = maximumCapacityCubicFeet;
	}
    
}
