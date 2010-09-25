//$Id: MutableNaturalIdTest.java 11645 2007-06-06 21:33:31Z steve.ebersole@jboss.com $
package org.hibernate.test.naturalid.mutable;

import java.lang.reflect.Field;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class MutableNaturalIdTest extends FunctionalTestCase {

	public MutableNaturalIdTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "naturalid/mutable/User.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true");
		cfg.setProperty(Environment.USE_QUERY_CACHE, "true");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MutableNaturalIdTest.class );
	}

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

	public void testNonexistentNaturalIdCache() {
		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 0 );

		s = openSession();
		t = s.beginTransaction();

		User u = new User("gavin", "hb", "secret");
		s.persist(u);

		t.commit();
		s.close();

		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );

		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 0 );

	}

	public void testNaturalIdCache() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "hb", "secret" );
		s.persist( u );
		t.commit();
		s.close();

		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		s = openSession();
		t = s.beginTransaction();
		User v = new User("xam", "hb", "foobar");
		s.persist(v);
		t.commit();
		s.close();

		getSessions().getStatistics().clear();

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
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );

		u = ( User ) s.createCriteria( User.class )
				.add( Restrictions.naturalId()
						.set("name", "gavin")
						.set("org", "hb")
				)
				.setCacheable( true )
				.uniqueResult();
		assertNotNull(u);
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery("delete User").executeUpdate();
		t.commit();
		s.close();
	}

	public void testNaturalIdDeleteUsingCache() {
		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hb", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		getSessions().getStatistics().clear();

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
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );

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

	public void testNaturalIdRecreateUsingCache() {
		testNaturalIdDeleteUsingCache();

		Session s = openSession();
		s.beginTransaction();
		User u = new User( "steve", "hb", "superSecret" );
		s.persist( u );
		s.getTransaction().commit();
		s.close();

		getSessions().getStatistics().clear();

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

		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCachePutCount(), 1 );

		getSessions().getStatistics().clear();
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
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 0 );
		assertEquals( getSessions().getStatistics().getQueryCacheHitCount(), 1 );

		s.delete( u );

		s.getTransaction().commit();
		s.close();
	}

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
}

