/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

/**
 * @author Andrea Boriero
 */
public class Order {

	private Long orderId;
	private OrderEntry firstOrder;

	public OrderEntry getFirstOrder() {
		return firstOrder;
	}

	protected void setFirstOrder(OrderEntry firstOrder) {
		this.firstOrder = firstOrder;
	}

	public void addFirstOrder(OrderEntry firstOrder){
		this.orderId = firstOrder.getEntryId();
		this.firstOrder = firstOrder;
	}


	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}
}
