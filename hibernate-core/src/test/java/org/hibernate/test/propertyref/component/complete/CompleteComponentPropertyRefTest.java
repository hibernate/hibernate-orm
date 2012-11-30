/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.propertyref.component.complete;


import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class CompleteComponentPropertyRefTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "propertyref/component/complete/Mapping.hbm.xml" };
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Test
	public void testComponentPropertyRef() {
		Session s = openSession();
		s.beginTransaction();
		Person p = new Person();
		p.setIdentity( new Identity() );
		Account a = new Account();
		a.setNumber("123-12345-1236");
		a.setOwner(p);
		p.getIdentity().setName("Gavin");
		p.getIdentity().setSsn("123-12-1234");
		s.persist(p);
		s.persist(a);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		a = (Account) s.createQuery("from Account a left join fetch a.owner").uniqueResult();
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );
		assertNotNull( a.getOwner() );
		assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
		s.clear();

		a = (Account) s.get(Account.class, "123-12345-1236");
		assertFalse( Hibernate.isInitialized( a.getOwner() ) );
		assertNotNull( a.getOwner() );
		assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );

		s.clear();

		sessionFactory().getCache().evictEntityRegion( Account.class );
		sessionFactory().getCache().evictEntityRegion( Person.class );

		a = (Account) s.get(Account.class, "123-12345-1236");
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );
		assertNotNull( a.getOwner() );
		assertEquals( "Gavin", a.getOwner().getIdentity().getName() );
		assertTrue( Hibernate.isInitialized( a.getOwner() ) );

		s.delete( a );
		s.delete( a.getOwner() );
		s.getTransaction().commit();
		s.close();
	}
}
