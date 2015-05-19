/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.keymanytoone.bidir.ondelete;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class Order implements Serializable {
	private Customer customer;
	private long number;
	private String item;

	public Order() {
	}

	public Order(Customer customer, long number) {
		this.customer = customer;
		this.number = number;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public long getNumber() {
		return number;
	}

	public void setNumber(long number) {
		this.number = number;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}
}
