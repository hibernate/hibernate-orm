/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking.paging;

import jakarta.persistence.LockModeType;
import org.hibernate.LockMode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Test of paging and locking in combination
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-1168")
@DomainModel(annotatedClasses = Door.class)
@SessionFactory
public class PagingAndLockingTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			session.persist( new Door( 1, "Front" ) );
			session.persist( new Door( 2, "Back" ) );
			session.persist( new Door( 3, "Garage" ) );
			session.persist( new Door( 4, "French" ) );
		} );
	}

	@AfterEach
	public void deleteTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			//noinspection deprecation
			var qry = session.createQuery( "from Door" );
			//noinspection removal
			qry.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
			qry.setFirstResult( 2 );
			qry.setMaxResults( 2 );
			@SuppressWarnings("unchecked") List<Door> results = qry.list();
			Assertions.assertEquals( 2, results.size() );
			for ( Door door : results ) {
				Assertions.assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
			}
		} );
	}

	@Test
	public void testCriteria(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(s -> {
			var criteriaBuilder = s.getCriteriaBuilder();
			var criteria = criteriaBuilder.createQuery( Door.class );
			criteria.from( Door.class );
			//noinspection removal
			var results = s.createQuery( criteria )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.setFirstResult( 2 )
					.setMaxResults( 2 )
					.list();
			Assertions.assertEquals( 2, results.size() );
			for ( Door door : results ) {
				Assertions.assertEquals( LockMode.PESSIMISTIC_WRITE, s.getCurrentLockMode( door ) );
			}
		} );
	}

	@Test
	public void testNativeSql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction(session -> {
			//noinspection deprecation
			var qry = session.createNativeQuery( "select * from door" );
			qry.addRoot( "door", Door.class );
			//noinspection removal
			qry.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
			qry.setFirstResult( 2 );
			qry.setMaxResults( 2 );
			List<?> results = qry.list();
			Assertions.assertEquals( 2, results.size() );
			for ( Object door : results ) {
				Assertions.assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
			}
		} );
		factoryScope.inTransaction(session -> {
			//noinspection deprecation
			var qry = session.createNativeQuery( "select * from door" );
			qry.addRoot( "door", Door.class );
			qry.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE );
			qry.setFirstResult( 2 );
			qry.setMaxResults( 2 );
			List<?> results = qry.list();
			Assertions.assertEquals( 2, results.size() );
			for ( Object door : results ) {
				Assertions.assertEquals( LockMode.PESSIMISTIC_WRITE, session.getCurrentLockMode( door ) );
			}
		} );
	}

}
