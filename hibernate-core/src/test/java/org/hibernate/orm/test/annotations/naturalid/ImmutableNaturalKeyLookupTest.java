/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.naturalid;

import jakarta.persistence.FlushModeType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Guenther Demetz
 */
public class ImmutableNaturalKeyLookupTest extends BaseCoreFunctionalTestCase {

	@JiraKey(value = "HHH-4838")
	@Test
	public void testSimpleImmutableNaturalKeyLookup() {
		Session s = openSession();
		Transaction newTx = s.getTransaction();

		newTx.begin();
		A a1 = new A();
		a1.setName( "name1" );
		s.persist( a1 );
		newTx.commit();

		newTx = s.beginTransaction();
		fetchA( s ); // put query-result into cache
		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		//Create new session to avoid the session cache which can't be tracked
		s.close();
		s = openSession();

		newTx = s.beginTransaction();

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		fetchA( s ); // should produce a hit in StandardQuery cache region

		Assert.assertEquals(
				"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions",
				1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount()
		);

		s.createQuery( "delete from A" ).executeUpdate();
		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testNaturalKeyLookupWithConstraint() {
		Session s = openSession();
		Transaction newTx = s.getTransaction();

		newTx.begin();
		A a1 = new A();
		a1.setName( "name1" );
		s.persist( a1 );
		newTx.commit();

		newTx = s.beginTransaction();

		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
		Root<A> root = criteria.from( A.class );
		criteria.where( criteriaBuilder.and(  criteriaBuilder.equal( root.get( "name" ), "name1" ), criteriaBuilder.isNull( root.get( "singleD" ) )) );

		Query<A> query = session.createQuery( criteria );
		query.setFlushMode( FlushModeType.COMMIT );
		query.setCacheable( true );
		query.uniqueResult(); // put query-result into cache

		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		newTx = s.beginTransaction();

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should not produce a hit in StandardQuery cache region because there is a constraint
		criteria = criteriaBuilder.createQuery( A.class );
		root = criteria.from( A.class );
		criteria.where( criteriaBuilder.and(  criteriaBuilder.equal( root.get( "name" ), "name1" ), criteriaBuilder.isNull( root.get( "singleD" ) )) );

		query = session.createQuery( criteria );
		query.setFlushMode( FlushModeType.COMMIT );
		query.setCacheable( true );
		query.uniqueResult();

		Assert.assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );

		s.createQuery( "delete from A" ).executeUpdate();
		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testCriteriaWithFetchModeJoinCollection() {
		Session s = openSession();
		Transaction newTx = s.getTransaction();

		newTx.begin();
		A a1 = new A();
		a1.setName( "name1" );
		D d1 = new D();
		a1.getDs().add( d1 );
		d1.setA( a1 );
		s.persist( d1 );
		s.persist( a1 );
		newTx.commit();

		newTx = s.beginTransaction();

		fetchA( s, "ds" ); // put query-result into cache

		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		//Create new session to avoid the session cache which can't be tracked
		s.close();
		s = openSession();

		newTx = s.beginTransaction();

		// please enable
		// logger.standard-query-cache.name=org.hibernate.cache.StandardQueryCache
		// logger.standard-query-cache.level=debug
		// logger.update-timestamps-cache.name=org.hibernate.cache.UpdateTimestampsCache
		// logger.update-timestamps-cache.level=debug
		// to see that isUpToDate is called where not appropriated

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should produce a hit in StandardQuery cache region
		fetchA( s, "ds" );

		Assert.assertEquals(
				"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions",
				1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount()
		);
		s.createQuery( "delete from D" ).executeUpdate();
		s.createQuery( "delete from A" ).executeUpdate();

		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testCriteriaWithFetchModeJoinOnetoOne() {
		Session s = openSession();
		Transaction newTx = s.getTransaction();

		newTx.begin();
		A a1 = new A();
		a1.setName( "name1" );
		D d1 = new D();
		a1.setSingleD( d1 );
		d1.setSingleA( a1 );
		s.persist( d1 );
		s.persist( a1 );
		newTx.commit();

		newTx = s.beginTransaction();
		fetchA( s, "singleD" ); // put query-result into cache

		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		//Create new session to avoid the session cache which can't be tracked
		s.close();
		s = openSession();

		newTx = s.beginTransaction();

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should produce a hit in StandardQuery cache region
		fetchA( s, "singleD" );

		Assert.assertEquals(
				"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions",
				1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount()
		);
		s.createQuery( "delete from A" ).executeUpdate();
		s.createQuery( "delete from D" ).executeUpdate();

		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testCriteriaWithAliasOneToOneJoin() {
		Session s = openSession();
		Transaction newTx = s.getTransaction();

		newTx.begin();
		A a1 = new A();
		a1.setName( "name1" );
		D d1 = new D();
		a1.setSingleD( d1 );
		d1.setSingleA( a1 );
		s.persist( d1 );
		s.persist( a1 );
		newTx.commit();

		newTx = s.beginTransaction();
		fetchA( s, "singleD" ); // put query-result into cache

		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		newTx = s.beginTransaction();

		// please enable
		// logger.standard-query-cache.name=org.hibernate.cache.StandardQueryCache
		// logger.standard-query-cache.level=debug
		// logger.update-timestamps-cache.name=org.hibernate.cache.UpdateTimestampsCache
		// logger.update-timestamps-cache.level=debug
		// to see that isUpToDate is called where not appropriated

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should not produce a hit in StandardQuery cache region because createAlias() creates a subcriteria
		fetchA( s, "singleD" );

		Assert.assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		s.createQuery( "delete from A" ).executeUpdate();
		s.createQuery( "delete from D" ).executeUpdate();

		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@JiraKey(value = "HHH-4838")
	@Test
	public void testSubCriteriaOneToOneJoin() {
		Session s = openSession();
		Transaction newTx = s.getTransaction();

		newTx.begin();
		A a1 = new A();
		a1.setName( "name1" );
		D d1 = new D();
		a1.setSingleD( d1 );
		d1.setSingleA( a1 );
		s.persist( d1 );
		s.persist( a1 );
		newTx.commit();

		newTx = s.beginTransaction();
		fetchA( s, "singleD" ); // put query-result into cache

		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		newTx = s.beginTransaction();

		// please enable
		// logger.standard-query-cache.name=org.hibernate.cache.StandardQueryCache
		// logger.standard-query-cache.level=debug
		// logger.update-timestamps-cache.name=org.hibernate.cache.UpdateTimestampsCache
		// logger.update-timestamps-cache.level=debug
		// to see that isUpToDate is called where not appropriated

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should not produce a hit in StandardQuery cache region because createCriteria() creates a subcriteria
		fetchA( s, "singleD" );

		Assert.assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		s.createQuery( "delete from A" ).executeUpdate();
		s.createQuery( "delete from D" ).executeUpdate();

		newTx.commit();
		// Shutting down the application
		s.close();
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				D.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, true );
		cfg.setProperty( Environment.USE_QUERY_CACHE, true );
	}


}
