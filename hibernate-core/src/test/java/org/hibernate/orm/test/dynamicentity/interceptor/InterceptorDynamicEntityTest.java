/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dynamicentity.interceptor;

import org.hibernate.Hibernate;
import org.hibernate.boot.SessionFactoryBuilder;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.test.dynamicentity.Company;
import org.hibernate.test.dynamicentity.Customer;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * @author Steve Ebersole
 * @see ProxyInterceptor
 */
public class InterceptorDynamicEntityTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/dynamicentity/interceptor/Customer.hbm.xml" };
	}

	@Override
	protected void configure(SessionFactoryBuilder builder) {
		builder.applyInterceptor( new ProxyInterceptor() );
	}

	@Test
	public void testIt() {
		// Test saving these dyna-proxies
		Company company = ProxyHelper.newCompanyProxy();
		Long customerId = fromTransaction(
				session -> {
					Customer customer = ProxyHelper.newCustomerProxy();
					company.setName( "acme" );
					session.save( company );
					customer.setName( "Steve" );
					customer.setCompany( company );
					session.save( customer );
					return customer.getId();
				}
		);

		assertNotNull( company.getId(), "company id not assigned" );
		assertNotNull( customerId, "customer id not assigned" );

		// Test loading these dyna-proxies, along with flush processing
		Customer customer = fromTransaction(
				session -> {
					Customer c = session.load( Customer.class, customerId );
					assertFalse( Hibernate.isInitialized( c ), "should-be-proxy was initialized" );

					c.setName( "other" );
					session.flush();
					assertFalse( Hibernate.isInitialized( c.getCompany() ), "should-be-proxy was initialized" );

					session.refresh( c );
					assertEquals( "other", c.getName(), "name not updated" );
					assertEquals( "acme", c.getCompany().getName(), "company association not correct" );
					return c;
				}
		);


		// Test detached entity re-attachment with these dyna-proxies
		customer.setName( "Steve" );
		inTransaction(
				session -> {
					session.update( customer );
					session.flush();
					session.refresh( customer );
					assertEquals( "Steve", customer.getName(), "name not updated" );
				}
		);

		// Test querying
		inTransaction(
				session -> {
					int count = session.createQuery( "from Customer" ).list().size();
					assertEquals( 1, count, "querying dynamic entity" );
					session.clear();
					count = session.createQuery( "from Person" ).list().size();
					assertEquals( 1, count, "querying dynamic entity" );
				}
		);

		// test deleteing
		inTransaction(
				session -> {
					session.delete( company );
					session.delete( customer );
				}
		);
	}

}
