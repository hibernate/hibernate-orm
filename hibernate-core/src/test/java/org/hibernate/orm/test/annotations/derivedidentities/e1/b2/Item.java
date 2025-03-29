/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b2;
import java.io.Serializable;
import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;



@NamedQueries( {
	@NamedQuery(name = "Item.findByCategory",
		query = "SELECT i FROM Item i WHERE i.category=:category ORDER BY i.id")})
@SuppressWarnings("serial")
@Entity
@Table(name = "O_ITEM")
public class Item implements Serializable
{

	public static final String QUERY_BY_CATEGORY = "Item.findByCategory";

	@Id
	@Column(name = "I_ID")
	private String id;

	@Column(name = "I_NAME")
	private String name;

	@Column(name = "I_PRICE")
	private BigDecimal price;

	@Column(name = "I_DESC")
	private String description;

	@Column(name = "I_DISCOUNT")
	private BigDecimal discount;

	@Column(name = "I_CATEGORY")
	private int category;

	@Version
	@Column(name = "I_VERSION")
	int version;

	public String getId() {
	return id;
	}

	public void setId(String i) {
	id = i;
	}

	public int getCategory() {
	return category;
	}

	public void setCategory(int category) {
	this.category = category;
	}

	public String getDescription() {
	return description;
	}

	public void setDescription(String description) {
	this.description = description;
	}

	public BigDecimal getDiscount() {
	return discount;
	}

	public void setDiscount(BigDecimal discount) {
	if (discount.doubleValue() < 0 || discount.doubleValue() > 100.0)
		throw new IllegalArgumentException(this + " discount " + discount
			+ " is invalid. Must be between 0.0 and 100.0");
	this.discount = discount;
	}

	public String getName() {
	return name;
	}

	public void setName(String name) {
	this.name = name;
	}

	public BigDecimal getPrice() {
	return price;
	}

	public void setPrice(BigDecimal price) {
	this.price = price;
	}

	public int getVersion() {
	return version;
	}

	@Override
	public boolean equals(Object other) {
	if (other == null || other.getClass() != this.getClass()) {
		return false;
	}
	if (other == this) {
		return true;
	}
	return id.equals(((Item) other).id);
	}

	@Override
	public int hashCode() {
	return id.hashCode();
	}
}
