/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dynamicentity.interceptor;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.dynamicentity.Company;
import org.hibernate.test.dynamicentity.Customer;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Demonstrates custom interpretation of entity-name through
 * an Interceptor.
 * <p/>
 * Here, we are generating dynamic
 * {@link java.lang.reflect.Proxy proxies} on the fly to represent
 * our entities.  Because of this, Hibernate would not be able to
 * determine the appropriate entity mapping to use given one of
 * these proxies (they are named like $Proxy1, or such).  Thus, we
 * plug a custom Interceptor into the session to perform this
 * entity-name interpretation.
 *
 * @see ProxyInterceptor
 *
 * @author Steve Ebersole
 */
public class InterceptorDynamicEntityTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "dynamicentity/interceptor/Customer.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setInterceptor( new ProxyInterceptor() );
	}

	@Test
	public void testIt() {
		// Test saving these dyna-proxies
		Session session = openSession();
		session.beginTransaction();
		Company company = ProxyHelper.newCompanyProxy();
		company.setName( "acme" );
		session.save( company );
		Customer customer = ProxyHelper.newCustomerProxy();
		customer.setName( "Steve" );
		customer.setCompany( company );
		session.save( customer );
		session.getTransaction().commit();
		session.close();

		assertNotNull( "company id not assigned", company.getId() );
		assertNotNull( "customer id not assigned", customer.getId() );

		// Test loading these dyna-proxies, along with flush processing
		session = openSession();
		session.beginTransaction();
		customer = ( Customer ) session.load( Customer.class, customer.getId() );
		assertFalse( "should-be-proxy was initialized", Hibernate.isInitialized( customer ) );

		customer.setName( "other" );
		session.flush();
		assertFalse( "should-be-proxy was initialized", Hibernate.isInitialized( customer.getCompany() ) );

		session.refresh( customer );
		assertEquals( "name not updated", "other", customer.getName() );
		assertEquals( "company association not correct", "acme", customer.getCompany().getName() );

		session.getTransaction().commit();
		session.close();

		// Test detached entity re-attachment with these dyna-proxies
		customer.setName( "Steve" );
		session = openSession();
		session.beginTransaction();
		session.update( customer );
		session.flush();
		session.refresh( customer );
		assertEquals( "name not updated", "Steve", customer.getName() );
		session.getTransaction().commit();
		session.close();

		// Test querying
		session = openSession();
		session.beginTransaction();
		int count = session.createQuery( "from Customer" ).list().size();
		assertEquals( "querying dynamic entity", 1, count );
		session.clear();
		count = session.createQuery( "from Person" ).list().size();
		assertEquals( "querying dynamic entity", 1, count );
		session.getTransaction().commit();
		session.close();

		// test deleteing
		session = openSession();
		session.beginTransaction();
		session.delete( company );
		session.delete( customer );
		session.getTransaction().commit();
		session.close();
	}

}
