/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idclass;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
public class IdClassTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "idclass/Customer.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		// the Customer entity has a composite id that is not embeddable ( not supported by JPA ).
		configuration.setProperty( AvailableSettings.JPA_METAMODEL_POPULATION, "disabled" );
	}

	@Test
	public void testIdClass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Customer cust = new FavoriteCustomer("JBoss", "RouteOne", "Detroit");
		s.persist(cust);
		t.commit();
		s.close();
		
		s = openSession();
		CustomerId custId = new CustomerId("JBoss", "RouteOne");
		t = s.beginTransaction();
		cust = (Customer) s.get(Customer.class, custId);
		assertEquals( "Detroit", cust.getAddress() );
		assertEquals( cust.getCustomerName(), custId.getCustomerName() );
		assertEquals( cust.getOrgName(), custId.getOrgName() );
		t.commit();
		s.close();		

		s = openSession();
		t = s.beginTransaction();
		cust = (Customer) s.createQuery("from Customer where id.customerName = 'RouteOne'").uniqueResult();
		assertEquals( "Detroit", cust.getAddress() );
		assertEquals( cust.getCustomerName(), custId.getCustomerName() );
		assertEquals( cust.getOrgName(), custId.getOrgName() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		cust = (Customer) s.createQuery("from Customer where customerName = 'RouteOne'").uniqueResult();
		assertEquals( "Detroit", cust.getAddress() );
		assertEquals( cust.getCustomerName(), custId.getCustomerName() );
		assertEquals( cust.getOrgName(), custId.getOrgName() );
		
		s.createQuery( "delete from Customer" ).executeUpdate();
		
		t.commit();
		s.close();
	}

}

