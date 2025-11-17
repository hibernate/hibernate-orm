/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.replace;

import org.hibernate.orm.test.jpa.callbacks.xml.common.CallbackTarget;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
@EntityListeners({ ListenerC.class, ListenerB.class})
public class Product extends CallbackTarget {
	@Id
	private Integer id;
	private String partNumber;
	private double cost;

	public Product() {
	}

	public Product(Integer id, String partNumber, double cost) {
		this.id = id;
		this.partNumber = partNumber;
		this.cost = cost;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}
}
