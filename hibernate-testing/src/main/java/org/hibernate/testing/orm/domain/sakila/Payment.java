/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author Nathan Xu
 */
@Entity
@Table(name = "payment")
public class Payment {
	private Integer id;
	private Customer customer;
	private Staff staff;
	private Rental rental;
	private BigDecimal amount;
	private LocalDateTime paymentDate;
	private LocalDateTime lastUpdate;

	public Payment() {
	}

	public Payment(
			Integer id,
			Customer customer,
			Staff staff,
			Rental rental,
			BigDecimal amount,
			LocalDateTime paymentDate) {
		this.id = id;
		this.customer = customer;
		this.staff = staff;
		this.rental = rental;
		this.amount = amount;
		this.paymentDate = paymentDate;
	}

	@Id
	@Column( name = "payment_id" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "customer_id" )
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	@ManyToOne( optional = false )
	@JoinColumn( name = "staff_id" )
	public Staff getStaff() {
		return staff;
	}

	public void setStaff(Staff staff) {
		this.staff = staff;
	}

	@OneToOne
	@JoinColumn( name = "rental_id" )
	public Rental getRental() {
		return rental;
	}

	public void setRental(Rental rental) {
		this.rental = rental;
	}

	@Column( nullable = false, precision = 5, scale = 2 )
	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	@Column( name = "payment_date", nullable = false )
	public LocalDateTime getPaymentDate() {
		return paymentDate;
	}

	public void setPaymentDate(LocalDateTime paymentDate) {
		this.paymentDate = paymentDate;
	}

	@Column( name = "last_update", nullable = false )
	public LocalDateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(LocalDateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
