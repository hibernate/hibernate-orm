/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class OrderedSetAggregateTest {

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
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithoutOrder(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<String> q = session.createQuery( "select listagg(eob.theString, ',') from EntityOfBasics eob", String.class );
					List<String> elements = Arrays.asList( q.getSingleResult().split( "," ) );
					List<String> expectedElements = List.of( "13", "5", "5", "6", "7" );
					elements.sort( String.CASE_INSENSITIVE_ORDER );

					assertEquals( expectedElements, elements );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testSimpleListagg(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<String> q = session.createQuery( "select listagg(eob.theString, ',') within group (order by eob.id desc) from EntityOfBasics eob", String.class );
					assertEquals( "5,13,7,6,5", q.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithFilter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<String> q = session.createQuery( "select listagg(eob.theString, ',') within group (order by eob.id desc) filter (where eob.theInt < 10) from EntityOfBasics eob", String.class );
					assertEquals( "5,7,6,5", q.getSingleResult() );
				}
		);
	}

	/*
	 * 	Skipped for MySQL 8.0: The test fails due to a regression in MySQL 8.0.x, which no longer supports NULLS FIRST/LAST in ORDER BY within LISTAGG as expected.
	 *	See https://bugs.mysql.com/bug.php?id=117765 for more details.
	 *	This is a MySQL issue, not a problem in the dialect implementation.
	 */
	@Test
	@JiraKey( value = "HHH-15360")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	@SkipForDialect(dialectClass = MySQLDialect.class, majorVersion = 8, reason = "https://bugs.mysql.com/bug.php?id=117765")
	@SkipForDialect(dialectClass = MySQLDialect.class, majorVersion = 9, reason = "https://bugs.mysql.com/bug.php?id=117765")
	public void testListaggWithNullsClause(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<String> q = session.createQuery( "select listagg(eob.theString, ',') within group (order by eob.id desc nulls first) from EntityOfBasics eob", String.class );
					assertEquals( "5,13,7,6,5", q.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsInverseDistributionFunctions.class)
	public void testInverseDistribution(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Integer> q = session.createQuery( "select percentile_disc(0.5) within group (order by eob.theInt asc) from EntityOfBasics eob", Integer.class );
					assertEquals( 6, q.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetPercentRank(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Double> q = session.createQuery( "select percent_rank(5) within group (order by eob.theInt asc) from EntityOfBasics eob", Double.class );
					assertEquals( 0.0D, q.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetRank(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Long> q = session.createQuery( "select rank(5) within group (order by eob.theInt asc) from EntityOfBasics eob", Long.class );
					assertEquals( 1L, q.getSingleResult() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetRankWithGroupByHavingOrderByLimit(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Tuple> q = session.createQuery( "select eob2.id, rank(5) within group (order by eob.theInt asc) from EntityOfBasics eob cross join EntityOfBasics eob2 group by eob2.id having eob2.id > 1 order by 1,2 offset 1", Tuple.class );
					List<Tuple> resultList = q.getResultList();
					assertEquals( 3, resultList.size() );
					assertEquals( 1L, resultList.get( 0 ).get( 1, Long.class ) );
					assertEquals( 1L, resultList.get( 1 ).get( 1, Long.class ) );
					assertEquals( 1L, resultList.get( 2 ).get( 1, Long.class ) );
				}
		);
	}
}
