/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.retail;

import java.time.Instant;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
