/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.test.naturalid.mutable;

import java.lang.reflect.Field;

import org.junit.Test;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

/**
 * @author Gavin King
 */
public class MutableNaturalIdTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "naturalid/mutable/User.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	@Test
	public void testCacheSynchronizationOnMutation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "hb", "secret" );
		s.persist( u );
		t.commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		u = (User) s.byId( User.class ).getReference( u.getId() );
		u.setOrg( "ceylon" );
		User oldNaturalId = (User) s.byNaturalId( User.class ).using( "name", "gavin" ).using( "org", "hb" ).load();
		assertNull( oldNaturalId );
		assertNotSame( u, oldNaturalId );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testReattachmentNaturalIdCheck() throws Throwable {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "gavin", "hb", "secret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		Field name = u.getClass().getDeclaredField("name");
		name.setAccessible(true);
		name.set(u, "Gavin");
		s = openSession();
		s.beginTransaction();
		try {
			s.update( u );
			assertNotNull(s.byNaturalId(User.class).using("name","Gavin").using("org", "hb").load());
			s.getTransaction().commit();
		}
		catch( HibernateException expected ) {
			s.getTransaction().rollback();
		}
		catch( Throwable t ) {
			try {
				s.getTransaction().rollback();
			}
			catch ( Throwable ignore ) {
			}
			throw t;
		}
		finally {
			s.close();
		}

		s = openSession();
		s.beginTransaction();
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}
	
	
	@Test
	public void testReattachmentUnmodifiedNaturalIdCheck() throws Throwable {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "gavin", "hb", "secret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		
		s = openSession();
		s.beginTransaction();
		try {
			s.buildLockRequest(LockOptions.NONE).lock(u);
			Field name = u.getClass().getDeclaredField("name");
			name.setAccessible(true);
			name.set(u, "Gavin");
			assertNotNull(s.byNaturalId(User.class).using("name","Gavin").using("org", "hb").load());
			s.getTransaction().commit();
		}
		catch( HibernateException expected ) {
			s.getTransaction().rollback();
		}
		catch( Throwable t ) {
			try {
				s.getTransaction().rollback();
			}
			catch ( Throwable ignore ) {
			}
			throw t;
		}
		finally {
			s.close();
		}

		s = openSession();
		s.beginTransaction();
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}
	

	@Test
	public void testNonexistentNaturalIdCache() {
		sessionFactory().getStatistics().clear();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		Object nullUser = s.createCriteria(User.class)
			.add( Restrictions.naturalId()
				.set("name", "gavin")
				.set("org", "hb")
			)
			.setCacheable(true)
			.uniqueResult();

		assertNull(nullUser);

		t.commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount(), 1 );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount(), 0 );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount(), 0 );

		s = openSession();
		t = s.beginTransaction();

		User u = new User("gavin", "hb", "secret");
		s.persist(u);

		t.commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		t = s.beginTransaction();

		u = (User) s.createCriteria(User.class)
			.add( Restrictions.naturalId()
				.set("name", "gavin")
				.set("org", "hb")
			)
			.setCacheable(true)
			.uniqueResult();

		assertNotNull(u);

		t.commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		sessionFactory().getStatistics().clear();

		s = openSession();
		t = s.beginTransaction();

		u = (User) s.createCriteria(User.class)
			.add( Restrictions.naturalId()
				.set("name", "gavin")
				.set("org", "hb")
			).setCacheable(true)
			.uniqueResult();

		s.delete(u);

		t.commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() ); //0: incorrect stats since hbm.xml can't enable NaturalId caching
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		sessionFactory().getStatistics().clear();

		s = openSession();
		t = s.beginTransaction();

		nullUser = s.createCriteria(User.class)
			.add( Restrictions.naturalId()
				.set("name", "gavin")
				.set("org", "hb")
			)
			.setCacheable(true)
			.uniqueResult();

		assertNull(nullUser);

		t.commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() );
	}

	@Test
	public void testNaturalIdCache() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "hb", "secret" );
		s.persist( u );
		t.commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		t = s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set( "name", "gavin" )
						.set( "org", "hb" )
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		t.commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		s = openSession();
		t = s.beginTransaction();
		User v = new User("xam", "hb", "foobar");
		s.persist(v);
		t.commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		t = s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "gavin")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull(u);
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );

		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "gavin")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull(u);
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete User").executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testNaturalIdDeleteUsingCache() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hb", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "steve")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		sessionFactory().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "steve")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() ); //0: incorrect stats since hbm.xml can't enable NaturalId caching
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		s.delete( u );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "steve")
						.set("org", "hb")
				)
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
		User u = new User( "steve", "hb", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getStatistics().clear();

		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "steve")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );

		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() );
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCachePutCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		sessionFactory().getStatistics().clear();
		s.getTransaction().commit();
		s.close();
		
		s = openSession();
		s.beginTransaction();
		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "steve")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull( u );
		assertEquals( 1, sessionFactory().getStatistics().getNaturalIdQueryExecutionCount() ); //0: incorrect stats since hbm.xml can't enable NaturalId caching
		assertEquals( 0, sessionFactory().getStatistics().getNaturalIdCacheHitCount() ); //1: no stats since hbm.xml can't enable NaturalId caching

		s.delete( u );

		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testQuerying() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		User u = new User("emmanuel", "hb", "bh");
		s.persist(u);

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		u = (User) s.createQuery( "from User u where u.name = :name" )
			.setParameter( "name", "emmanuel" ).uniqueResult();
		assertEquals( "emmanuel", u.getName() );
		s.delete( u );

		t.commit();
		s.close();
	}

	@Test
	public void testClear() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hb", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		u = (User) session.byNaturalId( User.class )
				.using( "name", "steve" )
				.using( "org", "hb" )
				.load();
		assertNotNull( u );
		s.clear();
		u = (User) session.byNaturalId( User.class )
				.using( "name", "steve" )
				.using( "org", "hb" )
				.load();
		assertNotNull( u );
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testEviction() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hb", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		u = (User) session.byNaturalId( User.class )
				.using( "name", "steve" )
				.using( "org", "hb" )
				.load();
		assertNotNull( u );
		s.evict( u );
		u = (User) session.byNaturalId( User.class )
				.using( "name", "steve" )
				.using( "org", "hb" )
				.load();
		assertNotNull( u );
		s.delete( u );
		s.getTransaction().commit();
		s.close();
	}
}
