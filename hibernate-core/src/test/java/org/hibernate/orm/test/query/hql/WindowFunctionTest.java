/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Date;
import java.util.List;

import org.hibernate.internal.util.ExceptionHelper;
import org.hibernate.query.SyntaxException;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Christian Beikov
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class WindowFunctionTest {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					Date now = new Date();

					EntityOfBasics entity1 = new EntityOfBasics();
					entity1.setId( 1 );
					entity1.setTheString( "5" );
					entity1.setTheInt( 5 );
					entity1.setTheInteger( -1 );
					entity1.setTheDouble( 5.0 );
					entity1.setTheDate( now );
					entity1.setTheBoolean( true );
					em.persist( entity1 );

					EntityOfBasics entity2 = new EntityOfBasics();
					entity2.setId( 2 );
					entity2.setTheString( "6" );
					entity2.setTheInt( 6 );
					entity2.setTheInteger( -2 );
					entity2.setTheDouble( 6.0 );
					entity2.setTheBoolean( true );
					em.persist( entity2 );

					EntityOfBasics entity3 = new EntityOfBasics();
					entity3.setId( 3 );
					entity3.setTheString( "7" );
					entity3.setTheInt( 7 );
					entity3.setTheInteger( 3 );
					entity3.setTheDouble( 7.0 );
					entity3.setTheBoolean( false );
					entity3.setTheDate( new Date(now.getTime() + 200000L) );
					em.persist( entity3 );

					EntityOfBasics entity4 = new EntityOfBasics();
					entity4.setId( 4 );
					entity4.setTheString( "13" );
					entity4.setTheInt( 13 );
					entity4.setTheInteger( 4 );
					entity4.setTheDouble( 13.0 );
					entity4.setTheBoolean( false );
					entity4.setTheDate( new Date(now.getTime() + 300000L) );
					em.persist( entity4 );

					EntityOfBasics entity5 = new EntityOfBasics();
					entity5.setId( 5 );
					entity5.setTheString( "5" );
					entity5.setTheInt( 5 );
					entity5.setTheInteger( 5 );
					entity5.setTheDouble( 9.0 );
					entity5.setTheBoolean( false );
					em.persist( entity5 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
	public void testRowNumberWithoutOrder(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Long> q = session.createQuery( "select row_number() over() from EntityOfBasics eob order by 1", Long.class );
					List<Long> resultList = q.getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 1L, resultList.get( 0 ) );
					assertEquals( 2L, resultList.get( 1 ) );
					assertEquals( 3L, resultList.get( 2 ) );
					assertEquals( 4L, resultList.get( 3 ) );
					assertEquals( 5L, resultList.get( 4 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
	public void testRank(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Long> q = session.createQuery( "select rank() over(partition by eob.theInt order by eob.id) from EntityOfBasics eob order by 1", Long.class );
					List<Long> resultList = q.getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 1L, resultList.get( 0 ) );
					assertEquals( 1L, resultList.get( 1 ) );
					assertEquals( 1L, resultList.get( 2 ) );
					assertEquals( 1L, resultList.get( 3 ) );
					assertEquals( 2L, resultList.get( 4 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
	public void testSumWithFilterAsWindowFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Long> q = session.createQuery( "select sum(eob.theInt) filter (where eob.theInt > 5) over (order by eob.theInt) from EntityOfBasics eob order by eob.theInt", Long.class );
					List<Long> resultList = q.getResultList();
					assertEquals( 5L, resultList.size() );
					assertNull( resultList.get( 0 ) );
					assertNull( resultList.get( 1 ) );
					assertEquals( 6L, resultList.get( 2 ) );
					assertEquals( 13L, resultList.get( 3 ) );
					assertEquals( 26L, resultList.get( 4 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
	public void testFrame(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Integer> q = session.createQuery( "select first_value(eob.theInt) over (order by eob.id rows between 2 preceding and current row) from EntityOfBasics eob order by eob.id", Integer.class );
					List<Integer> resultList = q.getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 5, resultList.get( 0 ) );
					assertEquals( 5, resultList.get( 1 ) );
					assertEquals( 5, resultList.get( 2 ) );
					assertEquals( 6, resultList.get( 3 ) );
					assertEquals( 7, resultList.get( 4 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsWindowFunctions.class )
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16347" )
	public void testOrderByAndAlias(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final TypedQuery<Integer> q = session.createQuery(
							"select id from (select id as id, dense_rank() over (order by theInt, id ASC) as ranking" +
							" from EntityOfBasics) entity_rank where ranking = :rank",
							Integer.class
					).setParameter( "rank", 5 );
					assertEquals( 4, q.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsWindowFunctions.class )
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16347" )
	public void testOrderByAndPositional(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"select id from (select id, dense_rank() over (order by theInt, 1 ASC) as ranking from EntityOfBasics) ",
						Integer.class
				);
				fail( "Order-by positional '1' should not be allowed in OVER clause" );
			}
			catch (Exception e) {
				final Throwable rootCause = ExceptionHelper.getRootCause( e );
				assertInstanceOf( SyntaxException.class, rootCause );
				assertEquals(
						"Position based 'order by' is not allowed in 'over' or 'within group' clauses",
						rootCause.getMessage()
				);
			}
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16655" )
	public void testParseWindowFrame(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
				session.createQuery(
						"select rank() over (order by theInt rows between current row and unbounded following) from EntityOfBasics",
						Long.class
				);
		} );
	}
}
