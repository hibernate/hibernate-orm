/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

public class CompositeDerivedIdentityTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Product.class,
				OrderLine.class,
				Order.class
		};
	}

	@Test
	public void testCreateProject() {
		Product product = new Product();
		product.setName("Product 1");

		Session session = openSession();
		session.beginTransaction();
		session.save(product);
		session.getTransaction().commit();
		session.close();

		Order order = new Order();
		order.setName("Order 1");
		order.addLineItem(product, 2);

		session = openSession();
		session.beginTransaction();
		session.save(order);
		session.getTransaction().commit();
		session.close();

		Long orderId = order.getId();

		session = openSession();
		session.beginTransaction();
		order = (Order) session.get(Order.class, orderId);
		assertEquals(1, order.getLineItems().size());
		session.delete( order );
		session.getTransaction().commit();
		session.close();
	}
}
