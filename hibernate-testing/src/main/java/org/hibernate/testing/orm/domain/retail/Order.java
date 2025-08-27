/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "orders")
public class Order {
	private Integer id;
	private Instant transacted;

	private Payment payment;
	private SalesAssociate salesAssociate;

	public Order() {
	}

	public Order(Integer id, Payment payment, SalesAssociate salesAssociate) {
		this( id, Instant.now(), payment, salesAssociate );
	}

	public Order(Integer id, Instant transacted, Payment payment, SalesAssociate salesAssociate) {
		this.id = id;
		this.transacted = transacted;
		this.payment = payment;
		this.salesAssociate = salesAssociate;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Instant getTransacted() {
		return transacted;
	}

	public void setTransacted(Instant transacted) {
		this.transacted = transacted;
	}

	@ManyToOne
	@JoinColumn(name = "payment_id")
	public Payment getPayment() {
		return payment;
	}

	public void setPayment(Payment payment) {
		this.payment = payment;
	}

	@ManyToOne
	@JoinColumn(name = "associate_id")
	public SalesAssociate getSalesAssociate() {
		return salesAssociate;
	}

	public void setSalesAssociate(SalesAssociate salesAssociate) {
		this.salesAssociate = salesAssociate;
	}

}
