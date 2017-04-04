/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.version;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7138" )
public class JpaSpecVersionValueUpdatingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class, Order.class };
	}

	@Test
	public void testVersionNotIncrementedOnModificationOfNonOwningCollectionNonCascaded() {
		Session session = openSession();
		session.beginTransaction();
		Customer customer = new Customer();
		customer.id = 1L;
		session.save( customer );
		session.getTransaction().commit();
		session.close();

		long initial = customer.version;

		session = openSession();
		session.beginTransaction();
		customer = (Customer) session.get( Customer.class, 1L );
		assertEquals( initial, customer.version );
		Order order = new Order();
		order.id = 1L;
		order.customer = customer;
		customer.orders.add( order );
		session.save( order );
		session.getTransaction().commit();
		session.close();

		assertEquals( initial, customer.version );

		session = openSession();
		session.beginTransaction();
		customer = (Customer) session.get( Customer.class, 1L );
		assertEquals( initial, customer.version );
		Order order2 = new Order();
		order2.id = 2L;
		order2.customer = customer;
		customer.orders.add( order2 );
		session.save( order2 );
		session.getTransaction().commit();
		session.close();

		assertEquals( initial, customer.version );

		session = openSession();
		session.beginTransaction();
		customer = (Customer) session.load( Customer.class, 1L );
		assertEquals( initial, customer.version );
		session.delete( customer );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testVersionNotIncrementedOnModificationOfNonOwningCollectionCascaded() {
		Customer customer = new Customer();
		customer.id = 1L;

		Session session = openSession();
		session.beginTransaction();
		session.save( customer );
		session.getTransaction().commit();
		session.close();

		long initial = customer.version;

		session = openSession();
		session.beginTransaction();
		customer = (Customer) session.get( Customer.class, 1L );
		assertEquals( initial, customer.version );
		Order order = new Order();
		order.id = 1L;
		order.customer = customer;
		customer.orders.add( order );
		session.getTransaction().commit();
		session.close();

		assertEquals( initial, customer.version );

		session = openSession();
		session.beginTransaction();
		customer = (Customer) session.get( Customer.class, 1L );
		Order order2 = new Order();
		order2.id = 2L;
		order2.customer = customer;
		customer.orders.add( order2 );
		session.getTransaction().commit();
		session.close();

		assertEquals( initial, customer.version );

		session = openSession();
		session.beginTransaction();
		customer = (Customer) session.load( Customer.class, 1L );
		assertEquals( initial, customer.version );
		session.delete( customer );
		session.getTransaction().commit();
		session.close();
	}
}
