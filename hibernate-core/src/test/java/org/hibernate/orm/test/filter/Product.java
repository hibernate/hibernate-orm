/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.filter;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Steve Ebersole
 */
public class Product {
	private Long id;
	private String name;
	private int stockNumber;  // int for ease of hashCode() impl
	private Date effectiveStartDate;
	private Date effectiveEndDate;
	private double weightPounds;
	private Set orderLineItems;
	private Set categories;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set getOrderLineItems() {
		return orderLineItems;
	}

	public void setOrderLineItems(Set orderLineItems) {
		this.orderLineItems = orderLineItems;
	}

	public int getStockNumber() {
		return stockNumber;
	}

	public void setStockNumber(int stockNumber) {
		this.stockNumber = stockNumber;
	}

	public int hashCode() {
		return stockNumber;
	}

	public boolean equals(Object obj) {
		return ( (Product) obj ).stockNumber == this.stockNumber;
	}

	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public Date getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(Date effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	public double getWeightPounds() {
		return weightPounds;
	}

	public void setWeightPounds(double weightPounds) {
		this.weightPounds = weightPounds;
	}

	public Set getCategories() {
		return categories;
	}

	public void setCategories(Set categories) {
		this.categories = categories;
	}

	public void addCategory(Category category) {
		if ( category == null ) {
			return;
		}

		if ( categories == null ) {
			categories = new HashSet();
		}

		categories.add( category );
		if ( category.getProducts() == null ) {
			category.setProducts( new HashSet() );
		}
		category.getProducts().add( this );
	}
}
