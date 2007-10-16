/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.test.naturalid.immutable;

import java.lang.reflect.Field;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ImmutableNaturalIdTest extends FunctionalTestCase {
	public ImmutableNaturalIdTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "naturalid/immutable/User.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( ImmutableNaturalIdTest.class );
	}

	public void configure(Configuration cfg) {
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
}
