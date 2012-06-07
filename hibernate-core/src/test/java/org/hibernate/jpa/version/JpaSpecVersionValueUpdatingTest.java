/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) ${year}, Red Hat Inc. or third-party contributors as
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
