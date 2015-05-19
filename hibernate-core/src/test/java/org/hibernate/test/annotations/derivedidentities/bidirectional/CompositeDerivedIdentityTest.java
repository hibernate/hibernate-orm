/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
