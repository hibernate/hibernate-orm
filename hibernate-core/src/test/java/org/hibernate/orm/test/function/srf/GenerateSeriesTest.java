/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.srf;

import jakarta.persistence.Tuple;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@DomainModel(standardModels = StandardDomainModel.LIBRARY)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsGenerateSeries.class)
public class GenerateSeriesTest {

	@BeforeAll
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Book(2, "Test") );
		} );
	}

	@AfterAll
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete Book" ).executeUpdate();
		} );
	}

	@Test
	public void testGenerateSeries(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-set-returning-function-generate-series-example[]
			List<Integer> resultList = em.createQuery( "select e from generate_series(1, 2) e order by e", Integer.class )
					.getResultList();
			//end::hql-set-returning-function-generate-series-example[]

			assertEquals( 2, resultList.size() );
			assertEquals( 1, resultList.get( 0 ) );
			assertEquals( 2, resultList.get( 1 ) );
		} );
	}

	@Test
	public void testNodeBuilderGenerateSeries(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
			final JpaFunctionRoot<Integer> root = cq.from( cb.generateSeries( 1, 2 ) );
			cq.select( root );
			cq.orderBy( cb.asc( root ) );
			List<Integer> resultList = em.createQuery( cq ).getResultList();

			assertEquals( 2, resultList.size() );
			assertEquals( 1, resultList.get( 0 ) );
			assertEquals( 2, resultList.get( 1 ) );
		} );
	}

	@Test
	public void testGenerateSeriesOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-set-returning-function-generate-series-ordinality-example[]
			List<Tuple> resultList = em.createQuery(
							"select index(e), e from generate_series(2, 3, 1) e order by index(e)",
							Tuple.class
					)
					.getResultList();
			//end::hql-set-returning-function-generate-series-ordinality-example[]

			assertEquals( 2, resultList.size() );
			assertEquals( 1L, resultList.get( 0 ).get( 0 ) );
			assertEquals( 2, resultList.get( 0 ).get( 1 ) );
			assertEquals( 2L, resultList.get( 1 ).get( 0 ) );
			assertEquals( 3, resultList.get( 1 ).get( 1 ) );
		} );
	}

	@Test
	public void testNodeBuilderGenerateSeriesOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaFunctionRoot<Integer> root = cq.from( cb.generateSeries( 2, 3, 1 ) );
			cq.multiselect( root.index(), root );
			cq.orderBy( cb.asc( root.index() ) );
			List<Tuple> resultList = em.createQuery( cq ).getResultList();

			assertEquals( 2, resultList.size() );
			assertEquals( 1L, resultList.get( 0 ).get( 0 ) );
			assertEquals( 2, resultList.get( 0 ).get( 1 ) );
			assertEquals( 2L, resultList.get( 1 ).get( 0 ) );
			assertEquals( 3, resultList.get( 1 ).get( 1 ) );
		} );
	}

	@Test
	public void testGenerateTimeSeries(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-set-returning-function-generate-series-temporal-example[]
			List<LocalDate> resultList = em.createQuery( "select e from generate_series(local date 2020-01-31, local date 2020-01-01, -1 day) e order by e", LocalDate.class )
					.getResultList();
			//end::hql-set-returning-function-generate-series-temporal-example[]

			assertEquals( 31, resultList.size() );
			for ( int i = 0; i < resultList.size(); i++ ) {
				assertEquals( LocalDate.of( 2020, Month.JANUARY, i +  1 ), resultList.get( i ) );
			}
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase bug?")
	public void testGenerateSeriesCorrelation(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Integer> resultList = em.createQuery(
							"select e from Book b join lateral generate_series(1,b.id) e order by e", Integer.class )
					.getResultList();

			assertEquals( 2, resultList.size() );
		} );
	}

	@Test
	public void testGenerateSeriesNegative(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Integer> resultList = em.createQuery( "select e from generate_series(2, 1, -1) e order by e", Integer.class )
					.getResultList();

			assertEquals( 2, resultList.size() );
			assertEquals( 1, resultList.get( 0 ) );
			assertEquals( 2, resultList.get( 1 ) );
		} );
	}

	@Test
	public void testGenerateSeriesNoProgression(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Integer> resultList = em.createQuery( "select e from generate_series(2, 1, 1) e", Integer.class )
					.getResultList();

			assertEquals( 0, resultList.size() );
		} );
	}

	@Test
	public void testGenerateSeriesNoProgressionOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Tuple> resultList = em.createQuery( "select index(e), e from generate_series(2, 1, 1) e", Tuple.class )
					.getResultList();

			assertEquals( 0, resultList.size() );
		} );
	}

	@Test
	public void testGenerateSeriesSameBounds(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Integer> resultList = em.createQuery( "select e from generate_series(2, 2, 1) e", Integer.class )
					.getResultList();

			assertEquals( 1, resultList.size() );
			assertEquals( 2, resultList.get( 0 ) );
		} );
	}

}
