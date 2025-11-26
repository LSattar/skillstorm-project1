package com.shelfsync.models;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "item")
public class Item {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int itemId;

	@Column(nullable = false, unique = true)
	private String sku;

	@Column(nullable = false)
	private String gameTitle;

	@ManyToOne
	@JoinColumn(name = "category_id")
	private Category category;

	@ManyToOne
	@JoinColumn(name = "company_id")
	private Company company;

	@Column(name = "weight_lbs", nullable = false)
	private BigDecimal weightLbs;

	@Column(nullable = false)
	private BigDecimal cubicFeet;

	public Item() {
	}

	public Item(int itemId, String sku, String gameTitle, Category category, Company company, BigDecimal weightLbs,
			BigDecimal cubicFeet) {
		super();
		this.itemId = itemId;
		this.sku = sku;
		this.gameTitle = gameTitle;
		this.category = category;
		this.company = company;
		this.weightLbs = weightLbs;
		this.cubicFeet = cubicFeet;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getGameTitle() {
		return gameTitle;
	}

	public void setGameTitle(String gameTitle) {
		this.gameTitle = gameTitle;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public BigDecimal getWeightLbs() {
		return weightLbs;
	}

	public void setWeightLbs(BigDecimal weightLbs) {
		this.weightLbs = weightLbs;
	}

	public BigDecimal getCubicFeet() {
		return cubicFeet;
	}

	public void setCubicFeet(BigDecimal cubicFeet) {
		this.cubicFeet = cubicFeet;
	}

}
