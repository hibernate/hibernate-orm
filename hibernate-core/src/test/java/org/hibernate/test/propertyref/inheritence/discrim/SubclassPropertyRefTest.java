/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.propertyref.inheritence.discrim;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class SubclassPropertyRefTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/inheritence/discrim/Person.hbm.xml" };
	}

	@Test
	public void testOneToOnePropertyRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Customer c = new Customer();
		c.setName( "Emmanuel" );
		c.setCustomerId( "C123-456" );
		c.setPersonId( "P123-456" );
		Account a = new Account();
		a.setCustomer( c );
		a.setPerson( c );
		a.setType( 'X' );
		s.persist( c );
		s.persist( a );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		a = ( Account ) s.createQuery( "from Account acc join fetch acc.customer join fetch acc.person" )
				.uniqueResult();
		assertNotNull( a.getCustomer() );
		assertTrue( Hibernate.isInitialized( a.getCustomer() ) );
		assertNotNull( a.getPerson() );
		assertTrue( Hibernate.isInitialized( a.getPerson() ) );
		c = ( Customer ) s.createQuery( "from Customer" ).uniqueResult();
		assertSame( c, a.getCustomer() );
		assertSame( c, a.getPerson() );
		s.delete( a );
		s.delete( a.getCustomer() );
		s.delete( a.getPerson() );
		t.commit();
		s.close();
	}

}

