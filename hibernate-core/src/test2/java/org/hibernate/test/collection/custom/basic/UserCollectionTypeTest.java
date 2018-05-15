/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.basic;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Max Rydahl Andersen
 */
public abstract class UserCollectionTypeTest extends BaseNonConfigCoreFunctionalTestCase {

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

