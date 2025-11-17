/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.column.transform;

/**
 * @author Steve Ebersole
 */
public class Item {
	private Integer id;
	private String name;
	private double cost;

	protected Item() {
		// for Hibernate use
	}

	public Item(Integer id, String name, double cost) {
		this.id = id;
		this.name = name;
		this.cost = cost;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}
}
