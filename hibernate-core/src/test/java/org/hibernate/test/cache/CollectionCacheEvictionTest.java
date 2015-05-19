/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Andreas Berger
 */
@TestForIssue(jiraKey = "HHH-4910")
public class CollectionCacheEvictionTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Company.class };
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

	@Test
	public void testCollectionCacheEvictionInsert() {
		Session s = openSession();
		s.beginTransaction();

		Company company = (Company) s.get( Company.class, 1 );
		// init cache of collection
		assertEquals( 1, company.getUsers().size() );

		User user = new User( 2, company );
		s.save( user );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		company = (Company) s.get( Company.class, 1 );
		// fails if cache is not evicted
		assertEquals( 2, company.getUsers().size() );

		s.close();
	}

	@Test
	public void testCollectionCacheEvictionRemove() {
		Session s = openSession();
		s.beginTransaction();

		Company company = (Company) s.get( Company.class, 1 );
		// init cache of collection
		assertEquals( 1, company.getUsers().size() );

		s.delete( company.getUsers().get( 0 ) );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		company = (Company) s.get( Company.class, 1 );
		// fails if cache is not evicted
		try {
			assertEquals( 0, company.getUsers().size() );
		}
		catch ( ObjectNotFoundException e ) {
			fail( "Cached element not found" );
		}
		s.close();
	}

	@Test
	public void testCollectionCacheEvictionUpdate() {
		Session s = openSession();
		s.beginTransaction();

		Company company1 = (Company) s.get( Company.class, 1 );
		Company company2 = (Company) s.get( Company.class, 2 );

		// init cache of collection
		assertEquals( 1, company1.getUsers().size() );
		assertEquals( 0, company2.getUsers().size() );

		User user = (User) s.get( User.class, 1 );
		user.setCompany( company2 );

		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();

		company1 = (Company) s.get( Company.class, 1 );
		company2 = (Company) s.get( Company.class, 2 );

		assertEquals( 1, company2.getUsers().size() );

		// fails if cache is not evicted
		try {
			assertEquals( 0, company1.getUsers().size() );
		}
		catch ( ObjectNotFoundException e ) {
			fail( "Cached element not found" );
		}

		s.close();
	}
}
