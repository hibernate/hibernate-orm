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
package org.hibernate.test.collection.custom.basic;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Max Rydahl Andersen
 */
public abstract class UserCollectionTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	public void testBasicOperation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User("max");
		u.getEmailAddresses().add( new Email("max@hibernate.org") );
		u.getEmailAddresses().add( new Email("max.andersen@jboss.com") );
		s.persist(u);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		User u2 = (User) s.createCriteria(User.class).uniqueResult();
		assertTrue( Hibernate.isInitialized( u2.getEmailAddresses() ) );
		assertEquals( u2.getEmailAddresses().size(), 2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u2 = ( User ) s.get( User.class, u.getUserName() );
		u2.getEmailAddresses().size();
		assertEquals( 2, MyListType.lastInstantiationRequest );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete( u );
		t.commit();
		s.close();
	}

}

