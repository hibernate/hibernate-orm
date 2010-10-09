/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.jpa.naturalid;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 * (copied from org.hibernate.test.naturalid.immutable.ImmutableNaturalIdTest)
 */
public class ImmutableNaturalIdTest extends AbstractJPATest {
	public ImmutableNaturalIdTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "jpa/naturalid/User.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ImmutableNaturalIdTest.class );
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public void testUpdate() {
		// prepare some test data...
		Session session = openSession();
    	session.beginTransaction();
	  	User user = new User();
    	user.setUserName( "steve" );
    	user.setEmail( "steve@hibernate.org" );
    	user.setPassword( "brewhaha" );
		session.save( user );
    	session.getTransaction().commit();
    	session.close();

		// 'user' is now a detached entity, so lets change a property and reattch...
		user.setPassword( "homebrew" );
		session = openSession();
		session.beginTransaction();
		session.update( user );
		session.getTransaction().commit();
		session.close();

		// clean up
		session = openSession();
		session.beginTransaction();
		session.delete( user );
		session.getTransaction().commit();
		session.close();
	}

	public void testNaturalIdCheck() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		User u = new User( "steve", "superSecret" );
		s.persist( u );
		u.setUserName( "Steve" );
		try {
			s.flush();
			fail();
		}
		catch ( HibernateException he ) {
		}
		u.setUserName( "steve" );
		s.delete( u );
		t.commit();
		s.close();
	}

	public void testNaturalIdCache() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		getSessions().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		s.getTransaction().commit();
		s.close();

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		s = openSession();
		s.beginTransaction();
		User v = new User( "gavin", "supsup" );
		s.persist( v );
		s.getTransaction().commit();
		s.close();

		getSessions().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 2 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete User" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	public void testNaturalIdDeleteUsingCache() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		getSessions().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		s.getTransaction().commit();
		s.close();

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		getSessions().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );

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

	public void testNaturalIdRecreateUsingCache() {
		testNaturalIdDeleteUsingCache();

		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		getSessions().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		getSessions().getStatistics().clear();
		s.getTransaction().commit();
		s.close();
		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId().set( "userName", "steve" ) )
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );

		s.delete( u );

		s.getTransaction().commit();
		s.close();
	}

}