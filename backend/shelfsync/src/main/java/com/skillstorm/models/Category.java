package com.skillstorm.models;

import jakarta.persistence.*;

@Entity
@Table(name = "category")
public class Category {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int categoryId;

	@Column(name = "category_name", nullable = false, unique = true)
	private String categoryName;

	public Category() {
	}

	public Category(int categoryId, String categoryName) {
		super();
		this.categoryId = categoryId;
		this.categoryName = categoryName;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public void setCategoryName(String categoryName) {
		this.categoryName = categoryName;
	}

}
