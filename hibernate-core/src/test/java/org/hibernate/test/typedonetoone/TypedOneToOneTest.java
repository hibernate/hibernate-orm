/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.typedonetoone;

import java.util.List;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class TypedOneToOneTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "typedonetoone/Customer.hbm.xml" };
	}

	@Test
	public void testCreateQuery() {
		Customer cust = new Customer();
		cust.setCustomerId("abc123");
		cust.setName("Matt");
		
		Address ship = new Address();
		ship.setStreet("peachtree rd");
		ship.setState("GA");
		ship.setCity("ATL");
		ship.setZip("30326");
		ship.setAddressId( new AddressId("SHIPPING", "abc123") );
		ship.setCustomer(cust);
		
		Address bill = new Address();
		bill.setStreet("peachtree rd");
		bill.setState("GA");
		bill.setCity("ATL");
		bill.setZip("30326");
		bill.setAddressId( new AddressId("BILLING", "abc123") );
		bill.setCustomer(cust);
		
		cust.setBillingAddress(bill);
		cust.setShippingAddress(ship);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(cust);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		List results = s.createQuery("from Customer cust left join fetch cust.billingAddress where cust.customerId='abc123'").list();
		//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
		cust = (Customer) results.get(0);
		assertTrue( Hibernate.isInitialized( cust.getShippingAddress() ) );
		assertTrue( Hibernate.isInitialized( cust.getBillingAddress() ) );
		assertEquals( "30326", cust.getBillingAddress().getZip() );
		assertEquals( "30326", cust.getShippingAddress().getZip() );
		assertEquals( "BILLING", cust.getBillingAddress().getAddressId().getType() );
		assertEquals( "SHIPPING", cust.getShippingAddress().getAddressId().getType() );
		s.delete( cust );
		t.commit();
		s.close();
	}

	@Test
	public void testCreateQueryNull() {
		Customer cust = new Customer();
		cust.setCustomerId("xyz123");
		cust.setName("Matt");
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(cust);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		List results = s.createQuery("from Customer cust left join fetch cust.billingAddress where cust.customerId='xyz123'").list();
		//List results = s.createQuery("from Customer cust left join fetch cust.billingAddress left join fetch cust.shippingAddress").list();
		cust = (Customer) results.get(0);
		assertNull( cust.getShippingAddress() );
		assertNull( cust.getBillingAddress() );
		s.delete(cust);
		t.commit();
		s.close();
		
	}

}

