/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.jpa.naturalid;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.jpa.AbstractJPATest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Andrea Boriero
 */
public class ImmutableNaturalIdUsingCacheTest extends AbstractJPATest {
	public String[] getMappings() {
		return new String[] { "jpa/naturalid/User.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testNaturalIdCache() {
		inTransaction(
				s -> {
					User u = new User( "steve", "superSecret" );
					s.persist( u );
				}
		);


		sessionFactory().getStatistics().clear();

		inTransaction(
				s -> {
					User u = ( User ) s.createCriteria( User.class )
							.add( Restrictions.naturalId().set( "userName", "steve" ) )
							.setCacheable( true )
							.uniqueResult();
					assertNotNull( u );
				}
		);

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() );//1: no stats since hbm.xml can't enable NaturalId caching

		inTransaction(
				s -> {
					User v = new User( "gavin", "supsup" );
					s.persist( v );
				}
		);

		sessionFactory().getStatistics().clear();

		inTransaction(
				s -> {
					User u = ( User ) s.createCriteria( User.class )
							.add( Restrictions.naturalId().set( "userName", "steve" ) )
							.setCacheable( true )
							.uniqueResult();
					assertNotNull( u );
					assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );//0: incorrect stats since hbm.xml can't enable NaturalId caching
					assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );//0: no stats since hbm.xml can't enable NaturalId caching
					u = ( User ) s.createCriteria( User.class )
							.add( Restrictions.naturalId().set( "userName", "steve" ) )
							.setCacheable( true )
							.uniqueResult();
					assertNotNull( u );
					assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );//0: incorrect stats since hbm.xml can't enable NaturalId caching
					assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );//0: no stats since hbm.xml can't enable NaturalId caching

				}
		);

		inTransaction(
				s -> s.createQuery( "delete User" ).executeUpdate()
		);
	}

	@Test
	public void testNaturalIdDeleteUsingCache() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() );//0: no stats since hbm.xml can't enable NaturalId caching

		sessionFactory().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );//0: incorrect stats since hbm.xml can't enable NaturalId caching
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );//1: incorrect stats since hbm.xml can't enable NaturalId caching

		s.delete( u );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNull( u );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testNaturalIdRecreateUsingCache() {
		testNaturalIdDeleteUsingCache();

		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() );//1: no stats since hbm.xml can't enable NaturalId caching

		sessionFactory().getStatistics().clear();
		s.getTransaction().commit();
		s.close();
		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );//0: incorrect stats since hbm.xml can't enable NaturalId caching
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );//1: incorrect stats since hbm.xml can't enable NaturalId caching

		s.delete( u );

		s.getTransaction().commit();
		s.close();
	}

}
