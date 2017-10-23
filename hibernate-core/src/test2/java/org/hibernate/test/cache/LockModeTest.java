package org.hibernate.test.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Guenther Demetz
 * @author Gail Badner
 */
public class LockModeTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Company.class };
	}

	@Before
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@After
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.AUTO_EVICT_COLLECTION_CACHE, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER_CONFIG, "true" );
	}

	@Override
	protected void prepareTest() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		Company company1 = new Company( 1 );
		s.save( company1 );

		User user = new User( 1, company1 );
		s.save( user );

		Company company2 = new Company( 2 );
		s.save( company2 );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		Session s = openSession();
		s.beginTransaction();

		s.createQuery( "delete from org.hibernate.test.cache.User" ).executeUpdate();
		s.createQuery( "delete from org.hibernate.test.cache.Company" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}

	/**
	 */
	@TestForIssue(jiraKey = "HHH-9764")
	@Test
	public void testDefaultLockModeOnCollectionInitialization() {
		Session s1 = openSession();
		s1.beginTransaction();

		Company company1 = s1.get( Company.class, 1 );

		User user1 = s1.get( User.class, 1 ); // into persistent context

		/******************************************
		 *
		 */
		Session s2 = openSession();
		s2.beginTransaction();
		User user = s2.get( User.class, 1 );
		user.setName("TestUser");
		s2.getTransaction().commit();
		s2.close();


		/******************************************
		 *
		 */

		// init cache of collection
		assertEquals( 1, company1.getUsers().size() ); // raises org.hibernate.StaleObjectStateException if 2LCache is enabled


		s1.getTransaction().commit();
		s1.close();
	}

	@TestForIssue(jiraKey = "HHH-9764")
	@Test
	public void testDefaultLockModeOnEntityLoad() {

		// first evict user
		sessionFactory().getCache().evictEntity( User.class.getName(), 1 );

		Session s1 = openSession();
		s1.beginTransaction();

		Company company1 = s1.get( Company.class, 1 );

		/******************************************
		 *
		 */
		Session s2 = openSession();
		s2.beginTransaction();
		Company company = s2.get( Company.class, 1 );
		company.setName( "TestCompany" );
		s2.getTransaction().commit();
		s2.close();


		/******************************************
		 *
		 */

		User user1 = s1.get( User.class, 1 ); // into persistent context

		// init cache of collection
		assertNull( user1.getCompany().getName() ); // raises org.hibernate.StaleObjectStateException if 2LCache is enabled

		s1.getTransaction().commit();
		s1.close();
	}

	@TestForIssue(jiraKey = "HHH-9764")
	@Test
	public void testReadLockModeOnEntityLoad() {

		// first evict user
		sessionFactory().getCache().evictEntity( User.class.getName(), 1 );

		Session s1 = openSession();
		s1.beginTransaction();

		Company company1 = s1.get( Company.class, 1 );

		/******************************************
		 *
		 */
		Session s2 = openSession();
		s2.beginTransaction();
		Company company = s2.get( Company.class, 1 );
		company.setName( "TestCompany" );
		s2.getTransaction().commit();
		s2.close();


		/******************************************
		 *
		 */

		User user1 = s1.get( User.class, 1, LockMode.READ ); // into persistent context

		// init cache of collection
		assertNull( user1.getCompany().getName() ); // raises org.hibernate.StaleObjectStateException if 2LCache is enabled

		s1.getTransaction().commit();
		s1.close();
	}

}
