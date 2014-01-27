/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.dynamicentity.tuplizer2;

import java.util.HashSet;

import org.junit.Test;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.dynamicentity.Address;
import org.hibernate.test.dynamicentity.Company;
import org.hibernate.test.dynamicentity.Customer;
import org.hibernate.test.dynamicentity.Person;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Demonstrates use of Tuplizers to allow the use of JDK
 * {@link java.lang.reflect.Proxy dynamic proxies} as our
 * domain model.
 * <p/>
 * Here we plug a custom Interceptor into the session simply to
 * allow us to not have to explicitly supply the appropriate entity
 * name to the Session calls.
 *
 * @author Steve Ebersole
 */
public class ImprovedTuplizerDynamicEntityTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "dynamicentity/tuplizer2/Customer.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.getEntityTuplizerFactory().registerDefaultTuplizerClass( EntityMode.POJO, MyEntityTuplizer.class );
	}

	@Test
	@SuppressWarnings( {"unchecked"})
	@FailureExpectedWithNewMetamodel
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
		Address address = ProxyHelper.newAddressProxy();
		address.setStreet( "somewhere over the rainbow" );
		address.setCity( "lawerence, kansas" );
		address.setPostalCode( "toto");
		customer.setAddress( address );
		customer.setFamily( new HashSet() );
		Person son = ProxyHelper.newPersonProxy();
		son.setName( "son" );
		customer.getFamily().add( son );
		Person wife = ProxyHelper.newPersonProxy();
		wife.setName( "wife" );
		customer.getFamily().add( wife );
		session.save( customer );
		session.getTransaction().commit();
		session.close();

		assertNotNull( "company id not assigned", company.getId() );
		assertNotNull( "customer id not assigned", customer.getId() );
		assertNotNull( "address id not assigned", address.getId() );
		assertNotNull( "son:Person id not assigned", son.getId() );
		assertNotNull( "wife:Person id not assigned", wife.getId() );

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
		assertEquals( "querying dynamic entity", 3, count );
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
