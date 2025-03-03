/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.version;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-7138" )
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
		session.persist( customer );
		session.getTransaction().commit();
		session.close();

		long initial = customer.version;

		session = openSession();
		session.beginTransaction();
		customer = session.get( Customer.class, 1L );
		assertEquals( initial, customer.version );
		Order order = new Order();
		order.id = 1L;
		order.customer = customer;
		customer.orders.add( order );
		session.persist( order );
		session.getTransaction().commit();
		session.close();

		assertEquals( initial, customer.version );

		session = openSession();
		session.beginTransaction();
		customer = session.get( Customer.class, 1L );
		assertEquals( initial, customer.version );
		Order order2 = new Order();
		order2.id = 2L;
		order2.customer = customer;
		customer.orders.add( order2 );
		session.persist( order2 );
		session.getTransaction().commit();
		session.close();

		assertEquals( initial, customer.version );

		session = openSession();
		session.beginTransaction();
		customer = session.getReference( Customer.class, 1L );
		assertEquals( initial, customer.version );
		session.remove( customer );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testVersionNotIncrementedOnModificationOfNonOwningCollectionCascaded() {
		Customer customer = new Customer();
		customer.id = 1L;

		Session session = openSession();
		session.beginTransaction();
		session.persist( customer );
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
		customer = session.getReference( Customer.class, 1L );
		assertEquals( initial, customer.version );
		session.remove( customer );
		session.getTransaction().commit();
		session.close();
	}
}
