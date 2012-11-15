/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.metamodel;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "CREDITCARD_TABLE")
public class CreditCard implements java.io.Serializable {
	private String id;
	private String number;
	private String type;
	private String expires;
	private boolean approved;
	private double balance;
	private Order order;
	private Customer customer;

	public CreditCard() {
	}

	public CreditCard(
			String v1, String v2, String v3, String v4,
			boolean v5, double v6, Order v7, Customer v8) {
		id = v1;
		number = v2;
		type = v3;
		expires = v4;
		approved = v5;
		balance = v6;
		order = v7;
		customer = v8;
	}

	public CreditCard(
			String v1, String v2, String v3, String v4,
			boolean v5, double v6) {
		id = v1;
		number = v2;
		type = v3;
		expires = v4;
		approved = v5;
		balance = v6;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "CREDITCARD_NUMBER")
	public String getNumber() {
		return number;
	}

	public void setNumber(String v) {
		number = v;
	}

	@Column(name = "`TYPE`")
	public String getType() {
		return type;
	}

	public void setType(String v) {
		type = v;
	}

	@Column(name = "EXPIRES")
	public String getExpires() {
		return expires;
	}

	public void setExpires(String v) {
		expires = v;
	}

	@Column(name = "APPROVED")
	public boolean getApproved() {
		return approved;
	}

	public void setApproved(boolean v) {
		approved = v;
	}

	@Column(name = "BALANCE")
	public double getBalance() {
		return balance;
	}

	public void setBalance(double v) {
		balance = v;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_FOR_ORDER_TABLE")
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order v) {
		order = v;
	}

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK3_FOR_CUSTOMER_TABLE")
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer v) {
		customer = v;
	}
}
