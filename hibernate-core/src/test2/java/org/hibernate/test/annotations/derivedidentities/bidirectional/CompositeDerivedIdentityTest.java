/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionImpl;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

	@Test
	@TestForIssue( jiraKey = "HHH-10476")
	public void testBidirectonalKeyManyToOneId() {
		Product product = new Product();
		product.setName( "Product 1" );

		Session session = openSession();
		session.beginTransaction();
		session.save( product );
		session.getTransaction().commit();
		session.close();

		Order order = new Order();
		order.setName( "Order 1" );
		order.addLineItem( product, 2 );

		session = openSession();
		session.beginTransaction();
		session.save( order );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		OrderLine orderLine = order.getLineItems().iterator().next();
		orderLine.setAmount( 5 );
		OrderLine orderLineGotten = session.get( OrderLine.class, orderLine );
		assertSame( orderLineGotten, orderLine );
		assertEquals( Integer.valueOf( 2 ), orderLineGotten.getAmount() );
		SessionImplementor si = (SessionImplementor) session;
		assertTrue( si.getPersistenceContext().isEntryFor( orderLineGotten ) );
		assertFalse( si.getPersistenceContext().isEntryFor( orderLineGotten.getOrder() ) );
		assertFalse( si.getPersistenceContext().isEntryFor( orderLineGotten.getProduct() ) );
		session.getTransaction().commit();
		session.close();
	}
}
