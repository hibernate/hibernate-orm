/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.subselect;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;


/**
 * @author Sharath Reddy
 */
@Entity
public class Bid {

	private int id;
	private long itemId;
	private double amount;

	@Id
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public long getItemId() {
		return itemId;
	}
	public void setItemId(long itemId) {
		this.itemId = itemId;
	}
	public double getAmount() {
		return amount;
	}
	public void setAmount(double val) {
		this.amount = val;
	}



}
