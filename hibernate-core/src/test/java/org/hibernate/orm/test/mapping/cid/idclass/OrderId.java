/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.cid.idclass;

/**
 * @author Steve Ebersole
 */
public class OrderId {
	Integer customer;
	Integer orderNumber;

	public OrderId() {
	}

	public OrderId(Integer customer, Integer orderNumber) {
		this.customer = customer;
		this.orderNumber = orderNumber;
	}
}
