/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;

import java.io.Serializable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


/**
 * Discount ticket a client can use when buying tickets
 *
 * @author Emmanuel Bernard
 */
@Entity
public class Discount implements Serializable {

	private Long id;
	private double discount;
	private Customer owner;


	@Column(precision = 5)
	public double getDiscount() {
		return discount;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setDiscount(double i) {
		discount = i;
	}

	public void setId(Long long1) {
		id = long1;
	}

	@ManyToOne(cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
	@JoinColumn(name = "CUSTOMER_ID")
	public Customer getOwner() {
		return owner;
	}

	public void setOwner(Customer customer) {
		owner = customer;
	}

}
