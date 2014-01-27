/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Defines an entity with an embedded id.
 *
 * @author Steve Ebersole
 */
@Entity
@Table( name = "T_ORDER" )
public class Order {
	private OrderPk id;

	@EmbeddedId
	public OrderPk getId() {
		return id;
	}

	@Embeddable
	public static class OrderPk {
		private String customerIdentifier;
		private int orderNumber;

		OrderPk() {
		}

		public OrderPk(String customerIdentifier, int orderNumber) {
			this.customerIdentifier = customerIdentifier;
			this.orderNumber = orderNumber;
		}

		public String getCustomerIdentifier() {
			return customerIdentifier;
		}

		private void setCustomerIdentifier(String customerIdentifier) {
			this.customerIdentifier = customerIdentifier;
		}

		public int getOrderNumber() {
			return orderNumber;
		}

		private void setOrderNumber(int orderNumber) {
			this.orderNumber = orderNumber;
		}
	}
}
