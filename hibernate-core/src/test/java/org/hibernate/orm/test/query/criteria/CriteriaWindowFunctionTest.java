/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.Date;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaWindow;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Marco Belladelli
 */
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CRITERIA_COPY_TREE, value = "true"))
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsWindowFunctions.class)
public class CriteriaWindowFunctionTest {
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
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testRowNumberWithoutOrder(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Long> cr = cb.createQuery( Long.class );
					cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow();
					JpaExpression<Long> rowNumber = cb.rowNumber( window );

					cr.select( rowNumber ).orderBy( cb.asc( cb.literal( 1 ) ) );
					List<Long> resultList = session.createQuery( cr ).getResultList();
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
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17391" )
	public void testRowNumberMultiSelectGroupBy(final SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final CriteriaQuery<Tuple> cr = cb.createQuery( Tuple.class );
					final Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					final JpaWindow window = cb.createWindow();
					window.partitionBy( root.get( "id" ) ).orderBy( cb.asc( root.get( "id" ) ) );
					final JpaExpression<Long> rowNumber = cb.rowNumber( window );

					cr.multiselect( root.get( "id" ), rowNumber ).groupBy( root.get( "id" ) );
					final List<Tuple> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5, resultList.size() );
					resultList.forEach( tuple -> assertEquals( 1L, tuple.get( 1, Long.class ) ) );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17392" )
	public void testRowNumberMultiSelect(final SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final CriteriaQuery<Tuple> cr = cb.createQuery( Tuple.class );
					final Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					final JpaWindow window = cb.createWindow();
					window.partitionBy( root.get( "id" ) ).orderBy( cb.asc( root.get( "id" ) ) );
					final JpaExpression<Long> rowNumber = cb.rowNumber( window );

					cr.multiselect( root.get( "id" ), rowNumber );
					final List<Tuple> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5, resultList.size() );
					resultList.forEach( tuple -> assertEquals( 1L, tuple.get( 1, Long.class ) ) );
				}
		);
	}

	@Test
	public void testFirstValue(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Integer> cr = cb.createQuery( Integer.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow().orderBy( cb.desc( root.get( "theInt" ) ) );
					JpaExpression<Integer> firstValue = cb.firstValue( root.get( "theInt" ), window );

					cr.select( firstValue ).orderBy( cb.asc( cb.literal( 1 ) ) );
					List<Integer> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 13, resultList.get( 0 ) );
					assertEquals( 13, resultList.get( 1 ) );
					assertEquals( 13, resultList.get( 2 ) );
					assertEquals( 13, resultList.get( 3 ) );
					assertEquals( 13, resultList.get( 4 ) );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SQLServerDialect.class, reason = "No support for nth_value function")
	@SkipForDialect(dialectClass = DB2Dialect.class, majorVersion = 10, reason = "No support for nth_value function")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "No support for nth_value function")
	public void testNthValue(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Integer> cr = cb.createQuery( Integer.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow()
							.orderBy( cb.desc( root.get( "theInt" ) ) )
							.frameRows( cb.frameUnboundedPreceding(), cb.frameUnboundedFollowing() );
					JpaExpression<Integer> nthValue = cb.nthValue(
							root.get( "theInt" ),
							2,
							window
					);

					cr.select( nthValue );
					List<Integer> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 7, resultList.get( 0 ) );
					assertEquals( 7, resultList.get( 1 ) );
					assertEquals( 7, resultList.get( 2 ) );
					assertEquals( 7, resultList.get( 3 ) );
					assertEquals( 7, resultList.get( 4 ) );
				}
		);
	}

	@Test
	public void testRank(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Long> cr = cb.createQuery( Long.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow()
							.partitionBy( root.get( "theInt" ) )
							.orderBy( cb.asc( root.get( "id" ) ) );
					JpaExpression<Long> rank = cb.rank( window );

					cr.select( rank ).orderBy( cb.asc( cb.literal( 1 ) ) );
					List<Long> resultList = session.createQuery( cr ).getResultList();
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
	@SkipForDialect(dialectClass = DB2Dialect.class, majorVersion = 10, reason = "No support for percent_rank and cume_dist functions before DB2 11")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "No support for percent_rank and cume_dist functions with over clause")
	public void testReusableWindow(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Tuple> cr = cb.createTupleQuery();
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow()
							.partitionBy( root.get( "theInt" ) )
							.orderBy( cb.asc( root.get( "id" ) ) );
					JpaExpression<Long> rowNumber = cb.rowNumber( window );
					JpaExpression<Double> percentRank = cb.percentRank( window );
					JpaExpression<Double> cumeDist = cb.cumeDist( window );

					cr.multiselect( rowNumber, percentRank, cumeDist ).orderBy( cb.asc( cb.literal( 1 ) ) );
					List<Tuple> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 0D, resultList.get( 0 ).get( 1 ) );
					assertEquals( 0D, resultList.get( 1 ).get( 1 ) );
					assertEquals( 0D, resultList.get( 2 ).get( 1 ) );
					assertEquals( 0D, resultList.get( 3 ).get( 1 ) );
					assertEquals( 1D, resultList.get( 4 ).get( 1 ) );
					assertEquals( 1D, resultList.get( 4 ).get( 2 ) );
				}
		);
	}

	@Test // TODO: currently fails on Informix but I don't know why
	public void testSumWithFilterAndWindow(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Long> cr = cb.createQuery( Long.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					Path<Integer> theInt = root.get( "theInt" );
					JpaWindow window = cb.createWindow().orderBy( cb.asc( theInt ) );
					JpaExpression<Long> sum = cb.sum( theInt, cb.gt( theInt, 5 ), window ).asLong();

					cr.select( sum ).orderBy( cb.asc( theInt, true ) );

					List<Long> resultList = session.createQuery( cr ).getResultList();
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
	public void testAvgAsWindowFunctionWithoutFilter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Double> cr = cb.createQuery( Double.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					Path<Integer> id = root.get( "id" );
					JpaWindow window = cb.createWindow().orderBy( cb.asc( id ) );
					JpaExpression<Double> avg = cb.avg( id, window );

					cr.select( avg ).orderBy( cb.asc( cb.literal( 1 ) ) );

					List<Double> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5L, resultList.size() );
					assertEquals( 1.0, resultList.get( 0 ) );
					assertEquals( 1.5, resultList.get( 1 ) );
					assertEquals( 2.0, resultList.get( 2 ) );
					assertEquals( 2.5, resultList.get( 3 ) );
					assertEquals( 3.0, resultList.get( 4 ) );
				}
		);
	}

	@Test
	public void testCountAsWindowFunctionWithFilter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Long> cr = cb.createQuery( Long.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow();
					JpaExpression<Long> count = cb.count( root, cb.gt( root.get( "id" ), 2 ), window );

					cr.select( count );

					List<Long> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5L, resultList.size() );
					assertEquals( 3L, resultList.get( 0 ) );
					assertEquals( 3L, resultList.get( 1 ) );
					assertEquals( 3L, resultList.get( 2 ) );
					assertEquals( 3L, resultList.get( 3 ) );
					assertEquals( 3L, resultList.get( 4 ) );
				}
		);
	}

	@Test
	public void testFrame(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Integer> cr = cb.createQuery( Integer.class );
					Root<EntityOfBasics> root = cr.from( EntityOfBasics.class );

					JpaWindow window = cb.createWindow()
							.orderBy( cb.asc( root.get( "id" ) ) )
							.frameRows( cb.frameBetweenPreceding( 2 ), cb.frameCurrentRow() );
					JpaExpression<Integer> firstValue = cb.firstValue( root.get( "theInt" ), window );

					cr.select( firstValue ).orderBy( cb.asc( root.get( "id" ) ) );

					List<Integer> resultList = session.createQuery( cr ).getResultList();
					assertEquals( 5, resultList.size() );
					assertEquals( 5, resultList.get( 0 ) );
					assertEquals( 5, resultList.get( 1 ) );
					assertEquals( 5, resultList.get( 2 ) );
					assertEquals( 6, resultList.get( 3 ) );
					assertEquals( 7, resultList.get( 4 ) );
				}
		);
	}
}
