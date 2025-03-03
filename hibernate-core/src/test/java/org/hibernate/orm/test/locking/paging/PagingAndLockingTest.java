/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.paging;

import java.util.List;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.LockMode;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test of paging and locking in combination
 *
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-1168")
public class PagingAndLockingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Door.class };
	}

	@Before
	public void createTestData() {
		inTransaction(
				session -> {
					session.persist( new Door( 1, "Front" ) );
					session.persist( new Door( 2, "Back" ) );
					session.persist( new Door( 3, "Garage" ) );
					session.persist( new Door( 4, "French" ) );

				}
		);
	}

	@After
	public void deleteTestData() {
		inTransaction(
				s -> s.createQuery( "delete Door" ).executeUpdate()
		);
	}

	@Test
	public void testHql() {
		inTransaction(
				session -> {
					Query qry = session.createQuery( "from Door" );
					qry.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
					qry.setFirstResult( 2 );
					qry.setMaxResults( 2 );
					@SuppressWarnings("unchecked") List<Door> results = qry.list();
					assertEquals( 2, results.size() );
					for ( Door door : results ) {
						assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
					}
				}
		);
	}

	@Test
	public void testCriteria() {
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Door> criteria = criteriaBuilder.createQuery( Door.class );
					criteria.from( Door.class );
//					Criteria criteria = session.createCriteria( Door.class );
//					criteria.setLockMode( LockMode.PESSIMISTIC_WRITE );
//					criteria.setFirstResult( 2 );
//					criteria.setMaxResults( 2 );
					List<Door> results = s.createQuery( criteria )
							.setLockMode( LockModeType.PESSIMISTIC_WRITE )
							.setFirstResult( 2 )
							.setMaxResults( 2 )
							.list();
					assertEquals( 2, results.size() );
					for ( Door door : results ) {
						assertEquals( LockMode.PESSIMISTIC_WRITE, s.getCurrentLockMode( door ) );
					}
				}
		);
	}

	@Test
//	@Ignore( "Support for locking on native-sql queries not yet implemented" )
	public void testNativeSql() {
		inTransaction(
				session -> {
					NativeQuery qry = session.createNativeQuery( "select * from door" );
					qry.addRoot( "door", Door.class );
					qry.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
					qry.setFirstResult( 2 );
					qry.setMaxResults( 2 );
					List results = qry.list();
					assertEquals( 2, results.size() );
					for ( Object door : results ) {
						assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
					}
				}
		);
		inTransaction(
				session -> {
					NativeQuery qry = session.createNativeQuery( "select * from door" );
					qry.addRoot( "door", Door.class );
					qry.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE );
					qry.setFirstResult( 2 );
					qry.setMaxResults( 2 );
					List results = qry.list();
					assertEquals( 2, results.size() );
					for ( Object door : results ) {
						assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
					}
				}
		);
	}

}
