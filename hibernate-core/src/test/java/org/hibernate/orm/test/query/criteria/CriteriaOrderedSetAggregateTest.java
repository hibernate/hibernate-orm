/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import jakarta.persistence.criteria.Nulls;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaWindow;
import org.hibernate.query.SortDirection;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@ServiceRegistry
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
public class CriteriaOrderedSetAggregateTest {
	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
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
			entity3.setTheDate( new Date( now.getTime() + 200000L ) );
			em.persist( entity3 );

			EntityOfBasics entity4 = new EntityOfBasics();
			entity4.setId( 4 );
			entity4.setTheString( "13" );
			entity4.setTheInt( 13 );
			entity4.setTheInteger( 4 );
			entity4.setTheDouble( 13.0 );
			entity4.setTheBoolean( false );
			entity4.setTheDate( new Date( now.getTime() + 300000L ) );
			em.persist( entity4 );

			EntityOfBasics entity5 = new EntityOfBasics();
			entity5.setId( 5 );
			entity5.setTheString( "5" );
			entity5.setTheInt( 5 );
			entity5.setTheInteger( 5 );
			entity5.setTheDouble( 9.0 );
			entity5.setTheBoolean( false );
			em.persist( entity5 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithoutOrder(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg( null, root.get( "theString" ), "," );

			cr.select( function );
			List<String> elements = Arrays.asList( session.createQuery( cr ).getSingleResult().split( "," ) );
			List<String> expectedElements = List.of( "13", "5", "5", "6", "7" );
			elements.sort( String.CASE_INSENSITIVE_ORDER );

			assertEquals( expectedElements, elements );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListagg(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ) ),
					root.get( "theString" ),
					","
			);

			cr.select( function );
			String result = session.createQuery( cr ).getSingleResult();
			assertEquals( "5,13,7,6,5", result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	public void testListaggWithFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ) ),
					cb.lt( root.get( "theInt" ), cb.literal( 10 ) ),
					root.get( "theString" ),
					","
			);

			cr.select( function );
			String result = session.createQuery( cr ).getSingleResult();
			assertEquals( "5,7,6,5", result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
	@RequiresDialect(H2Dialect.class)
	public void testListaggWithFilterAndWindow(SessionFactoryScope scope) {
		// note : not many dbs support this for now but the generated sql is correct
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaWindow window = cb.createWindow().partitionBy( root.get( "theInt" ) );
			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ) ),
					cb.lt( root.get( "theInt" ), cb.literal( 10 ) ),
					window,
					root.get( "theString" ),
					","
			);

			cr.select( function );
			List<String> resultList = session.createQuery( cr ).getResultList();
			assertEquals( "5,5", resultList.get( 0 ) );
			assertEquals( "6", resultList.get( 1 ) );
			assertEquals( "7", resultList.get( 2 ) );
			assertNull( resultList.get( 3 ) );
			assertEquals( "5,5", resultList.get( 4 ) );
		} );
	}

	/*
	 * 	Skipped for MySQL 8.0: The test fails due to a regression in MySQL 8.0.x, which no longer supports NULLS FIRST/LAST in ORDER BY within LISTAGG as expected.
	 *	See https://bugs.mysql.com/bug.php?id=117765 for more details.
	 *	This is a MySQL issue, not a problem in the dialect implementation.
	 */
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStringAggregation.class)
	@SkipForDialect(dialectClass = MySQLDialect.class, majorVersion = 8, reason = "https://bugs.mysql.com/bug.php?id=117765")
	@SkipForDialect(dialectClass = MySQLDialect.class, majorVersion = 9, reason = "https://bugs.mysql.com/bug.php?id=117765")
	public void testListaggWithNullsClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<String> cr = cb.createQuery( String.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<String> function = cb.listagg(
					cb.desc( root.get( "id" ), true ),
					root.get( "theString" ),
					cb.literal( "," )
			);

			cr.select( function );
			String result = session.createQuery( cr ).getSingleResult();
			assertEquals( "5,13,7,6,5", result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsInverseDistributionFunctions.class)
	public void testInverseDistribution(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Integer> cr = cb.createQuery( Integer.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<Integer> function = cb.percentileDisc(
					cb.literal( 0.5 ),
					root.get( "theInt" ),
					SortDirection.ASCENDING,
					Nulls.NONE
			);

			cr.select( function );
			Integer result = session.createQuery( cr ).getSingleResult();
			assertEquals( 6, result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsInverseDistributionFunctions.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
	@SkipForDialect(dialectClass = PostgreSQLDialect.class)
	@SkipForDialect(dialectClass = CockroachDialect.class)
	@SkipForDialect(dialectClass = PostgresPlusDialect.class)
	public void testInverseDistributionWithWindow(SessionFactoryScope scope) {
		// note : PostgreSQL, CockroachDB and EDB currently do not support
		// ordered-set aggregate functions with OVER clause
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Integer> cr = cb.createQuery( Integer.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaWindow window = cb.createWindow().partitionBy( root.get( "theInt" ) );
			JpaExpression<Integer> function = cb.percentileDisc(
					cb.literal( 0.5 ),
					window,
					root.get( "theInt" ),
					SortDirection.ASCENDING,
					Nulls.NONE
			);

			cr.select( function ).orderBy( cb.asc( cb.literal( 1 ) ) );
			List<Integer> resultList = session.createQuery( cr ).getResultList();
			assertEquals( 5, resultList.size() );
			assertEquals( 5, resultList.get( 0 ) );
			assertEquals( 5, resultList.get( 1 ) );
			assertEquals( 6, resultList.get( 2 ) );
			assertEquals( 7, resultList.get( 3 ) );
			assertEquals( 13, resultList.get( 4 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetPercentRank(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Double> cr = cb.createQuery( Double.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<Double> function = cb.percentRank( cb.asc( root.get( "theInt" ) ), cb.literal( 5 ) );

			cr.select( function );
			Double result = session.createQuery( cr ).getSingleResult();
			assertEquals( 0.0D, result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	public void testHypotheticalSetRank(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Long> cr = cb.createQuery( Long.class );
			Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

			JpaExpression<Long> function = cb.rank( cb.asc( root.get( "theInt" ) ), cb.literal( 5 ) );

			cr.select( function );
			Long result = session.createQuery( cr ).getSingleResult();
			assertEquals( 1L, result );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsHypotheticalSetFunctions.class)
	@RequiresDialect(H2Dialect.class)
	public void testHypotheticalSetRankWithGroupByHavingOrderByLimit(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> cr = cb.createQuery( Tuple.class );
			JpaRoot<EntityOfBasics> e1 = (JpaRoot<EntityOfBasics>) cr.from( EntityOfBasics.class );
			JpaCrossJoin<EntityOfBasics> e2 = e1.crossJoin( EntityOfBasics.class );

			JpaExpression<Long> function = cb.rank( cb.asc( e1.get( "theInt" ) ), cb.literal( 5 ) );

			cr.multiselect( e2.get( "id" ), function )
					.groupBy( e2.get( "id" ) ).having( cb.gt( e2.get( "id" ), cb.literal( 1 ) ) )
					.orderBy( cb.asc( cb.literal( 1 ) ), cb.asc( cb.literal( 2 ) ) );

			List<Tuple> resultList = session.createQuery( cr ).setFirstResult( 1 ).getResultList();
			assertEquals( 3, resultList.size() );
			assertEquals( 1L, resultList.get( 0 ).get( 1, Long.class ) );
			assertEquals( 1L, resultList.get( 1 ).get( 1, Long.class ) );
			assertEquals( 1L, resultList.get( 2 ).get( 1, Long.class ) );
		} );
	}
}
