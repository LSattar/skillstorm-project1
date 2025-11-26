package com.shelfsync.models;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "employee")
public class Employee {

	@Id
	@Column(name = "employee_id", columnDefinition = "UUID")
	private UUID employeeId;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "first_name", nullable = false)
	private String firstName;

	@Column(name = "last_name", nullable = false)
	private String lastName;

	@Column(name = "phone", nullable = false)
	private String phone;

	@Column(name = "email", unique = true)
	private String email;

	@ManyToOne
	@JoinColumn(name = "assigned_warehouse_id")
	private Warehouse assignedWarehouse;

	@PrePersist
	public void generateUUID() {
		if (employeeId == null) {
			employeeId = UUID.randomUUID();
		}
	}

	public Employee() {
	}

	public Employee(String passwordHash, String firstName, String lastName, String phone, String email,
			Warehouse assignedWarehouse) {
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
		this.email = email;
		this.assignedWarehouse = assignedWarehouse;
	}

	public UUID getEmployeeId() {
		return employeeId;
	}

	public void setEmployeeId(UUID employeeId) {
		this.employeeId = employeeId;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Warehouse getAssignedWarehouse() {
		return assignedWarehouse;
	}

	public void setAssignedWarehouse(Warehouse assignedWarehouse) {
		this.assignedWarehouse = assignedWarehouse;
	}

}
