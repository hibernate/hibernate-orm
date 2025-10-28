/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import jakarta.persistence.FlushModeType;
import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.StatisticsSettings.GENERATE_STATISTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Guenther Demetz
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name=GENERATE_STATISTICS, value="true"),
		@Setting(name=USE_QUERY_CACHE, value="true")
})
@DomainModel(annotatedClasses = {A.class, D.class})
@SessionFactory
public class ImmutableNaturalKeyLookupTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testSimpleImmutableNaturalKeyLookup(SessionFactoryScope factoryScope) {
		assertTrue( factoryScope.getSessionFactory().getStatistics().isStatisticsEnabled() );

		factoryScope.inTransaction( (s) -> {
			A a1 = new A();
			a1.setName( "name1" );
			s.persist( a1 );
		} );

		factoryScope.inTransaction( (s) -> {
			// put query-result into cache
			fetchA( s );

			A a2 = new A();
			a2.setName( "xxxxxx" );
			s.persist( a2 );
		} );

		factoryScope.getSessionFactory().getStatistics().clear();

		factoryScope.inTransaction( (s) -> {
			// should produce a hit in StandardQuery cache region
			fetchA( s );

			assertEquals( 1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount(),
					"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions" );
		} );
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testNaturalKeyLookupWithConstraint(SessionFactoryScope factoryScope) {
		assertTrue( factoryScope.getSessionFactory().getStatistics().isStatisticsEnabled() );

		factoryScope.inTransaction( (s) -> {
			A a1 = new A();
			a1.setName( "name1" );
			s.persist( a1 );
		} );

		final var criteriaBuilder = factoryScope.getSessionFactory().getCriteriaBuilder();

		factoryScope.inTransaction( (s) -> {
			var criteria = criteriaBuilder.createQuery( A.class );
			var root = criteria.from( A.class );
			criteria.where( criteriaBuilder.and(  criteriaBuilder.equal( root.get( "name" ), "name1" ), criteriaBuilder.isNull( root.get( "singleD" ) )) );

			// put query-result into cache
			s.createQuery( criteria )
					.setFlushMode( FlushModeType.COMMIT )
					.setCacheable( true )
					.uniqueResult();
		} );

		factoryScope.inTransaction( (s) -> {
			// Invalidates space A in UpdateTimeStamps region
			A a2 = new A();
			a2.setName( "xxxxxx" );
			s.persist( a2 );
		} );

		factoryScope.getSessionFactory().getStatistics().clear();

		factoryScope.inTransaction( (s) -> {
			// should not produce a hit in StandardQuery cache region because there is a constraint

			var criteria = criteriaBuilder.createQuery( A.class );
			var root = criteria.from( A.class );
			criteria.where( criteriaBuilder.and(  criteriaBuilder.equal( root.get( "name" ), "name1" ), criteriaBuilder.isNull( root.get( "singleD" ) )) );

			s.createQuery( criteria )
					.setFlushMode( FlushModeType.COMMIT )
					.setCacheable( true )
					.uniqueResult();

			assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		} );
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testCriteriaWithFetchModeJoinCollection(SessionFactoryScope factoryScope) {
		assertTrue( factoryScope.getSessionFactory().getStatistics().isStatisticsEnabled() );

		factoryScope.inTransaction( (s) -> {
			A a1 = new A();
			a1.setName( "name1" );
			D d1 = new D();
			a1.getDs().add( d1 );
			d1.setA( a1 );
			s.persist( d1 );
			s.persist( a1 );
		} );

		factoryScope.inTransaction( (s) -> {
			// put query-result into cache
			fetchA( s, "ds" );

			A a2 = new A();
			a2.setName( "xxxxxx" );
			// Invalidates space A in UpdateTimeStamps region
			s.persist( a2 );
		} );

		factoryScope.inTransaction( (s) -> {
			s.getSessionFactory().getStatistics().clear();

			// should produce a hit in StandardQuery cache region
			fetchA( s, "ds" );

			assertEquals( 1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount(),
					"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions" );
		} );
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testCriteriaWithFetchModeJoinOneToOne(SessionFactoryScope factoryScope) {
		assertTrue( factoryScope.getSessionFactory().getStatistics().isStatisticsEnabled() );

		factoryScope.inTransaction( (s) -> {
			A a1 = new A();
			a1.setName( "name1" );
			D d1 = new D();
			a1.setSingleD( d1 );
			d1.setSingleA( a1 );
			s.persist( d1 );
			s.persist( a1 );
		} );

		factoryScope.inTransaction( (s) -> {
			// put query-result into cache
			fetchA( s, "singleD" );

			// Invalidates space A in UpdateTimeStamps region
			A a2 = new A();
			a2.setName( "xxxxxx" );
			s.persist( a2 );
		} );

		factoryScope.inTransaction( (s) -> {
			s.getSessionFactory().getStatistics().clear();

			// should produce a hit in StandardQuery cache region
			fetchA( s, "singleD" );

			assertEquals( 1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount(),
					"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions" );
		} );
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testCriteriaWithAliasOneToOneJoin(SessionFactoryScope factoryScope) {
		assertTrue( factoryScope.getSessionFactory().getStatistics().isStatisticsEnabled() );

		factoryScope.inTransaction( (s) -> {
			A a1 = new A();
			a1.setName( "name1" );
			D d1 = new D();
			a1.setSingleD( d1 );
			d1.setSingleA( a1 );
			s.persist( d1 );
			s.persist( a1 );
		} );

		factoryScope.inTransaction( (s) -> {
			// put query-result into cache
			fetchA( s, "singleD" );

			// Invalidates space A in UpdateTimeStamps region
			A a2 = new A();
			a2.setName( "xxxxxx" );
			s.persist( a2 );
		} );

		factoryScope.inTransaction( (s) -> {
			s.getSessionFactory().getStatistics().clear();

			// should not produce a hit in StandardQuery cache region because createAlias() creates a subcriteria
			fetchA( s, "singleD" );

			assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		} );
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testSubCriteriaOneToOneJoin(SessionFactoryScope factoryScope) {
		assertTrue( factoryScope.getSessionFactory().getStatistics().isStatisticsEnabled() );

		factoryScope.inTransaction( (s) -> {
			A a1 = new A();
			a1.setName( "name1" );
			D d1 = new D();
			a1.setSingleD( d1 );
			d1.setSingleA( a1 );
			s.persist( d1 );
			s.persist( a1 );
		} );

		factoryScope.inTransaction( (s) -> {
			// put query-result into cache
			fetchA( s, "singleD" );

			// Invalidates space A in UpdateTimeStamps region
			A a2 = new A();
			a2.setName( "xxxxxx" );
			s.persist( a2 );
		} );

		factoryScope.inTransaction( (s) -> {
			s.getSessionFactory().getStatistics().clear();

			// should not produce a hit in StandardQuery cache region because createCriteria() creates a subcriteria
			fetchA( s, "singleD" );

			assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		} );
	}

	private A fetchA(Session s) {
		return s.byNaturalId( A.class )
				.using( "name", "name1" )
				.load();
	}

	private A fetchA(Session s, String fetch) {
		final RootGraph<A> entityGraph = s.createEntityGraph( A.class );
		entityGraph.addAttributeNodes( fetch );
		( (SharedSessionContractImplementor) s ).getLoadQueryInfluencers()
				.getEffectiveEntityGraph()
				.applyGraph( (RootGraphImplementor<?>) entityGraph, GraphSemantic.LOAD );
		final A a = s.byNaturalId( A.class )
				.using( "name", "name1" )
				.load();
		((SharedSessionContractImplementor) s).getLoadQueryInfluencers().getEffectiveEntityGraph().clear();
		return a;
	}


}
