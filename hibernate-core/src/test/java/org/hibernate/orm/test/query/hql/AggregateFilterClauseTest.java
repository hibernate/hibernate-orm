/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Date;

import org.hibernate.query.Query;
import org.hibernate.query.SyntaxException;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Jan Schatteman
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class AggregateFilterClauseTest {

	@BeforeAll
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					Date now = new Date();

					EntityOfBasics entity1 = new EntityOfBasics();
					entity1.setId( 1 );
					entity1.setTheInt( 5 );
					entity1.setTheInteger( -1 );
					entity1.setTheDouble( 5.0 );
					entity1.setTheDate( now );
					entity1.setTheBoolean( true );
					em.persist( entity1 );

					EntityOfBasics entity2 = new EntityOfBasics();
					entity2.setId( 2 );
					entity2.setTheInt( 6 );
					entity2.setTheInteger( -2 );
					entity2.setTheDouble( 6.0 );
					entity2.setTheBoolean( true );
					em.persist( entity2 );

					EntityOfBasics entity3 = new EntityOfBasics();
					entity3.setId( 3 );
					entity3.setTheInt( 7 );
					entity3.setTheInteger( 3 );
					entity3.setTheDouble( 7.0 );
					entity3.setTheBoolean( false );
					entity3.setTheDate( new Date(now.getTime() + 200000L) );
					em.persist( entity3 );

					EntityOfBasics entity4 = new EntityOfBasics();
					entity4.setId( 4 );
					entity4.setTheInt( 13 );
					entity4.setTheInteger( 4 );
					entity4.setTheDouble( 13.0 );
					entity4.setTheBoolean( false );
					entity4.setTheDate( new Date(now.getTime() + 300000L) );
					em.persist( entity4 );

					EntityOfBasics entity5 = new EntityOfBasics();
					entity5.setId( 5 );
					entity5.setTheInteger( 5 );
					entity5.setTheDouble( 9.0 );
					entity5.setTheBoolean( false );
					em.persist( entity5 );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testSimpleSum(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Long expected = 31L;
					Query q = session.createQuery( "select sum(eob.theInt) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testSumWithFilterClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Long expected = 11L;
					Query q = session.createQuery( "select sum(eob.theInt) filter(where eob.theBoolean = true) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testSimpleAvg(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double expected = 8.0D;
					Query q = session.createQuery( "select avg(eob.theDouble) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testAvgWithFilterClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double expected = 5.5D;
					Query q = session.createQuery( "select avg(eob.theDouble) filter(where eob.theBoolean = true) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testSimpleMin(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double expected = 5D;
					Query q = session.createQuery( "select min(eob.theDouble) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testMinWithFilterClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double expected = 7D;
					Query q = session.createQuery( "select min(eob.theDouble) filter(where eob.theBoolean = false) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testSimpleMax(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double expected = 13D;
					Query q = session.createQuery( "select max(eob.theDouble) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testMaxWithFilterClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Double expected = 6D;
					Query q = session.createQuery( "select max(eob.theDouble) filter(where eob.theBoolean = true) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testSimpleCount(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Long expected = 5L;
					Query q = session.createQuery( "select count(*) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );

					expected = 3L;
					q = session.createQuery( "select count(eob.theDate) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	public void testCountWithFilterClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Long expected = 3L;
					Query q = session.createQuery( "select count(*) filter(where eob.theBoolean = false) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );

					expected = 2L;
					q = session.createQuery( "select count(eob.theDate) filter(where eob.theBoolean = false) from EntityOfBasics eob" );
					assertEquals( expected, q.getSingleResult(), "expected " + expected + ", got " + q.getSingleResult() );
				}
		);
	}

	@Test
	// poor test verification, but ok ...
	public void testSimpleEveryAll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "select every( eob.theInteger > 0 ) from EntityOfBasics eob" );
					assertFalse( (Boolean) q.getSingleResult() );

					q = session.createQuery( "select any( eob.theInteger < 0 ) from EntityOfBasics eob" );
					assertTrue( (Boolean) q.getSingleResult() );
				}
		);
	}

	@Test
	// poor test verification, but ok ...
	public void testEveryAllWithFilterClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "select every( eob.theInteger > 0 ) filter ( where eob.theBoolean = false ) from EntityOfBasics eob" );
					assertTrue( (Boolean) q.getSingleResult() );

					q = session.createQuery( "select any( eob.theInteger < 0 ) filter ( where eob.theBoolean = false ) from EntityOfBasics eob" );
					assertFalse( (Boolean) q.getSingleResult() );
				}
		);
	}

	@Test
	public void testIllegalSubquery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Exception e = Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> {
								session.createQuery( "select every( select 1 ) filter ( where eob.theBoolean = false ) from EntityOfBasics eob" );
							}

					);
					assertEquals( SyntaxException.class, e.getCause().getClass() );
				}
		);
		scope.inTransaction(
				session -> {
					Exception e = Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> {
								session.createQuery( "select any( select 1 ) filter ( where eob.theBoolean = false ) from EntityOfBasics eob" );
							}

					);
					assertEquals( SyntaxException.class, e.getCause().getClass() );
				}
		);
	}
}
