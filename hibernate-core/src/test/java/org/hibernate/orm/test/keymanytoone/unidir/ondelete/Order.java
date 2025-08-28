/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.keymanytoone.unidir.ondelete;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak
 */
public class Order implements Serializable {
	private Id id;
	private String item;

	public Order() {
	}

	public Order(Id id) {
		this.id = id;
	}

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public static class Id implements Serializable {
		private Customer customer;
		private long number;

		public Id() {
		}

		public Id(Customer customer, long number) {
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

		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Id ) ) {
				return false;
			}

			Id id = ( Id ) o;
			return number == id.number && customer.equals( id.customer );
		}

		public int hashCode() {
			int result = customer.hashCode();
			result = 31 * result + ( int ) ( number ^ ( number >>> 32 ) );
			return result;
		}
	}
}
