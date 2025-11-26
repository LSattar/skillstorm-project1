package com.shelfsync.models;

import jakarta.persistence.*;

@Entity
@Table(name = "company")
public class Company {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int companyId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "phone")
	private String phone;

	@Column(name = "email")
	private String email;

	@Column(name = "contact_person")
	private String contactPerson;

	public Company() {
	}

	public Company(int companyId, String name, String phone, String email, String contactPerson) {
		super();
		this.companyId = companyId;
		this.name = name;
		this.phone = phone;
		this.email = email;
		this.contactPerson = contactPerson;
	}

	public int getCompanyId() {
		return companyId;
	}

	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public String getContactPerson() {
		return contactPerson;
	}

	public void setContactPerson(String contactPerson) {
		this.contactPerson = contactPerson;
	}

}
