/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 */
@Entity
public class FoodItem {
	private Integer id;
	private String item;
	private Menu order;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	@ManyToOne
	@JoinColumnOrFormula(column=@JoinColumn(name="order_nbr", referencedColumnName="order_nbr"))
	@JoinColumnOrFormula(formula=@JoinFormula(value="'F'", referencedColumnName="is_default"))
	public Menu getOrder() {
		return order;
	}

	public void setOrder(Menu order) {
		this.order = order;
	}
}
