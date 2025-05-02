/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;


/**
 * @author Gavin King
 */
public class Model {
	private Integer id;
	private String name;
	private String description;
	private ProductLine productLine;

	Model() {
	}

	public Model(String name, String description, ProductLine productLine) {
		this.name = name;
		this.description = description;
		this.productLine = productLine;
		productLine.getModels().add(this);
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ProductLine getProductLine() {
		return productLine;
	}
	public void setProductLine(ProductLine productLine) {
		this.productLine = productLine;
	}
}
