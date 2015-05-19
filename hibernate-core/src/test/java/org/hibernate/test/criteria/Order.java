/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Order {

	private int orderId;

	public int getOrderId() {
    return orderId;
  }

	private Set<OrderLine> orderLines = new HashSet<OrderLine>();

	public Set<OrderLine> getLines() {
    return Collections.unmodifiableSet(orderLines);
  }

	public void addLine(OrderLine orderLine){
		orderLine.setOrder(this);
		this.orderLines.add(orderLine);
	}

	private Set<OrderContact> orderContacts = new HashSet<OrderContact>();

	public Set<OrderContact> getContacts() {
		return Collections.unmodifiableSet(orderContacts);
	}

	public void addContact(OrderContact orderContact){
		orderContact.getOrders().add( this );
		this.orderContacts.add(orderContact);
	}

	public OrderAddress orderAddress;

	public OrderAddress getOrderAddress() {
		return orderAddress;
	}

	public void setOrderAddress(OrderAddress orderAddress) {
		this.orderAddress = orderAddress;
	}

	public String toString() {
    return "" + getOrderId() + " - " + getLines();
  }
}
