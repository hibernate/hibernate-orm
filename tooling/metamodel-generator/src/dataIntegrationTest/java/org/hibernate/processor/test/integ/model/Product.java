/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "integ_products")
public class Product {

	@Id
	Long id;

	String name;

	Double price;

	Double surgePrice;

	protected Product() {
	}

	public Product(Long id, String name, Double price, Double surgePrice) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.surgePrice = surgePrice;
	}

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

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Double getSurgePrice() {
		return surgePrice;
	}

	public void setSurgePrice(Double surgePrice) {
		this.surgePrice = surgePrice;
	}
}
