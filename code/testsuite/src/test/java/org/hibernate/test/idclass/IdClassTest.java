//$Id: IdClassTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.idclass;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class IdClassTest extends FunctionalTestCase {
	
	public IdClassTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "idclass/Customer.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( IdClassTest.class );
	}

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

