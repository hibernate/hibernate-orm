/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.locking.paging;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Test of paging and locking in combination
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-1168" )
public class PagingAndLockingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Door.class };
	}

	@Before
	public void createTestData() {
		Session session = openSession();
		session.beginTransaction();
		session.save( new Door( 1, "Front" ) );
		session.save( new Door( 2, "Back" ) );
		session.save( new Door( 3, "Garage" ) );
		session.save( new Door( 4, "French" ) );
		session.getTransaction().commit();
		session.close();
	}

	@After
	public void deleteTestData() {
		Session session = openSession();
		session.beginTransaction();
		session.createQuery( "delete Door" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testHql() {
		Session session = openSession();
		session.beginTransaction();
		Query qry = session.createQuery( "from Door" );
		qry.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
		qry.setFirstResult( 2 );
		qry.setMaxResults( 2 );
		@SuppressWarnings("unchecked") List<Door> results = qry.list();
		assertEquals( 2, results.size() );
		for ( Door door : results ) {
			assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testCriteria() {
		Session session = openSession();
		session.beginTransaction();
		Criteria criteria = session.createCriteria( Door.class );
		criteria.setLockMode( LockMode.PESSIMISTIC_WRITE );
		criteria.setFirstResult( 2 );
		criteria.setMaxResults( 2 );
		@SuppressWarnings("unchecked") List<Door> results = criteria.list();
		assertEquals( 2, results.size() );
		for ( Door door : results ) {
			assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
//	@Ignore( "Support for locking on native-sql queries not yet implemented" )
	public void testNativeSql() {
		Session session = openSession();
		session.beginTransaction();
		SQLQuery qry = session.createSQLQuery( "select * from door" );
		qry.addRoot( "door", Door.class );
		qry.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
		qry.setFirstResult( 2 );
		qry.setMaxResults( 2 );
		@SuppressWarnings("unchecked") List<Door> results = qry.list();
		assertEquals( 2, results.size() );
		for ( Door door : results ) {
			assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
		}
		session.getTransaction().commit();
		session.close();
	}

}
