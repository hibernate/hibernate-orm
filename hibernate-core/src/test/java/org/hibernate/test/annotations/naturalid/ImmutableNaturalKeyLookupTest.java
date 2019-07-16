/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.naturalid;

import javax.persistence.FlushModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.query.Query;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Guenther Demetz
 */
public class ImmutableNaturalKeyLookupTest extends BaseCoreFunctionalTestCase {

	@TestForIssue(jiraKey = "HHH-4838")
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
		getQuery( s ).uniqueResult(); // put query-result into cache
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

		getQuery( s ).uniqueResult(); // should produce a hit in StandardQuery cache region

		Assert.assertEquals(
				"query is not considered as isImmutableNaturalKeyLookup, despite fullfilling all conditions",
				1, s.getSessionFactory().getStatistics().getNaturalIdCacheHitCount()
		);

		s.createQuery( "delete from A" ).executeUpdate();
		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@TestForIssue(jiraKey = "HHH-4838")
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
		query.uniqueResult();
//		getCriteria( s ).add( Restrictions.isNull( "singleD" ) ).uniqueResult(); // put query-result into cache
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
//		getCriteria( s ).add( Restrictions.isNull( "singleD" ) ).uniqueResult();

		Assert.assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );

		s.createQuery( "delete from A" ).executeUpdate();
		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@TestForIssue(jiraKey = "HHH-4838")
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

		getQueryFecth( s, "ds", JoinType.LEFT ).uniqueResult();
//		getQuery( s ).setFetchMode( "ds", FetchMode.JOIN ).uniqueResult(); // put query-result into cache
		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region
		
		//Create new session to avoid the session cache which can't be tracked
		s.close();
		s = openSession();

		newTx = s.beginTransaction();

		// please enable
		// log4j.logger.org.hibernate.cache.StandardQueryCache=DEBUG
		// log4j.logger.org.hibernate.cache.UpdateTimestampsCache=DEBUG
		// to see that isUpToDate is called where not appropriated

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should produce a hit in StandardQuery cache region
		getQueryFecth( s, "ds", JoinType.LEFT ).uniqueResult();
//		getCriteria( s ).setFetchMode( "ds", FetchMode.JOIN ).uniqueResult();

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

	@TestForIssue(jiraKey = "HHH-4838")
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
		getQueryFecth( s, "singleD" , JoinType.LEFT).uniqueResult();
//		getCriteria( s ).setFetchMode( "singleD", FetchMode.JOIN ).uniqueResult(); // put query-result into cache
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
		getQueryFecth( s, "singleD", JoinType.LEFT ).uniqueResult();
//		getCriteria( s ).setFetchMode( "singleD", FetchMode.JOIN ).uniqueResult();

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

	@TestForIssue(jiraKey = "HHH-4838")
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
		getQueryFecth( s, "singleD", JoinType.LEFT).uniqueResult();
//		getCriteria( s ).createAlias( "singleD", "d", JoinType.LEFT_OUTER_JOIN )
//				.uniqueResult(); // put query-result into cache
		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		newTx = s.beginTransaction();

		// please enable
		// log4j.logger.org.hibernate.cache.StandardQueryCache=DEBUG
		// log4j.logger.org.hibernate.cache.UpdateTimestampsCache=DEBUG
		// to see that isUpToDate is called where not appropriated

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should not produce a hit in StandardQuery cache region because createAlias() creates a subcriteria
		getQueryFecth( s, "singleD", JoinType.LEFT).uniqueResult();
//		getCriteria( s ).createAlias( "singleD", "d", JoinType.LEFT_OUTER_JOIN ).uniqueResult();

		Assert.assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		s.createQuery( "delete from A" ).executeUpdate();
		s.createQuery( "delete from D" ).executeUpdate();

		newTx.commit();
		// Shutting down the application
		s.close();
	}

	@TestForIssue(jiraKey = "HHH-4838")
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
		getQueryFecth( s, "singleD", JoinType.LEFT).uniqueResult();

//		getCriteria( s ).createCriteria( "singleD", "d", JoinType.LEFT_OUTER_JOIN )
//				.uniqueResult(); // put query-result into cache
		A a2 = new A();
		a2.setName( "xxxxxx" );
		s.persist( a2 );
		newTx.commit();	  // Invalidates space A in UpdateTimeStamps region

		newTx = s.beginTransaction();

		// please enable
		// log4j.logger.org.hibernate.cache.StandardQueryCache=DEBUG
		// log4j.logger.org.hibernate.cache.UpdateTimestampsCache=DEBUG
		// to see that isUpToDate is called where not appropriated

		Assert.assertTrue( s.getSessionFactory().getStatistics().isStatisticsEnabled() );
		s.getSessionFactory().getStatistics().clear();

		// should not produce a hit in StandardQuery cache region because createCriteria() creates a subcriteria
		getQueryFecth( s, "singleD", JoinType.LEFT).uniqueResult();

//		getCriteria( s ).createCriteria( "singleD", "d", JoinType.LEFT_OUTER_JOIN ).uniqueResult();

		Assert.assertEquals( 0, s.getSessionFactory().getStatistics().getQueryCacheHitCount() );
		s.createQuery( "delete from A" ).executeUpdate();
		s.createQuery( "delete from D" ).executeUpdate();

		newTx.commit();
		// Shutting down the application
		s.close();
	}

	private Query<A> getQuery(Session s) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
		Root<A> root = criteria.from( A.class );
		criteria.where( criteriaBuilder.equal( root.get( "name" ), "name1" ) );

		Query<A> query = session.createQuery( criteria );
		query.setFlushMode( FlushModeType.COMMIT );
		query.setCacheable( true );
		return query;
	}

	private Query<A> getQueryFecth(Session s, String fecth, JoinType joinType) {
		CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
		CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
		Root<A> root = criteria.from( A.class );
		root.fetch( fecth, joinType );

		criteria.where( criteriaBuilder.equal( root.get( "name" ), "name1" ) );

		Query<A> query = session.createQuery( criteria );
		query.setFlushMode( FlushModeType.COMMIT );
		query.setCacheable( true );
		return query;
	}

//	private CriteriaQuery getCriteria(Session s) {
//		Criteria crit = s.createCriteria( A.class, "anAlias" );
//		crit.add( Restrictions.naturalId().set( "name", "name1" ) );
//		crit.setFlushMode( FlushMode.COMMIT );
//		crit.setCacheable( true );
//		return crit;
//	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				D.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
	}


}
