/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.propertyref.inheritence.union;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class UnionSubclassPropertyRefTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/inheritence/union/Person.hbm.xml" };
	}

	@Test
	@FailureExpectedWithNewMetamodel
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
