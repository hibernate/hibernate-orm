/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

/**
 * @author Lukasz Antoniak
 */
@Entity
@Table(name = "ITEMS")
@SecondaryTable(name="ITEMS_SEC")
public class Item implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	@Column(name = "price")
	private Double price;

	@Column(name = "price", table = "ITEMS_SEC")
	private Double specialPrice;

	public Item() {
	}

	public Item(Double price, Double specialPrice) {
		this.price = price;
		this.specialPrice = specialPrice;
	}

	public Item(Long id, Double price, Double specialPrice) {
		this.id = id;
		this.price = price;
		this.specialPrice = specialPrice;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Item item = (Item) o;

		if (id != null ? !id.equals(item.id) : item.id != null) return false;
		if (price != null ? !price.equals(item.price) : item.price != null) return false;
		if (specialPrice != null ? !specialPrice.equals(item.specialPrice) : item.specialPrice != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (price != null ? price.hashCode() : 0);
		result = 31 * result + (specialPrice != null ? specialPrice.hashCode() : 0);
		return result;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getSpecialPrice() {
		return specialPrice;
	}

	public void setSpecialPrice(Double specialPrice) {
		this.specialPrice = specialPrice;
	}
}
