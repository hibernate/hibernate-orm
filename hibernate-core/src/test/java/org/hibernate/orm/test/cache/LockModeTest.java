/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
		cfg.setProperty( Environment.AUTO_EVICT_COLLECTION_CACHE, true );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, true );
		cfg.setProperty( Environment.USE_QUERY_CACHE, true );
		cfg.setProperty( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Override
	protected void prepareTest() {
		inTransaction(
				s -> {
					Company company1 = new Company( 1 );
					s.persist( company1 );

					User user = new User( 1, company1 );
					s.persist( user );

					Company company2 = new Company( 2 );
					s.persist( company2 );
				}
		);
	}

	@Override
	protected void cleanupTest() {
		inTransaction(
				s -> {
					s.createQuery( "delete from org.hibernate.orm.test.cache.User" ).executeUpdate();
					s.createQuery( "delete from org.hibernate.orm.test.cache.Company" ).executeUpdate();

				}
		);
	}

	/**
	 */
	@JiraKey(value = "HHH-9764")
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

	@JiraKey(value = "HHH-9764")
	@Test
	public void testDefaultLockModeOnEntityLoad() {

		// first evict user
		sessionFactory().getCache().evictEntityData( User.class.getName(), 1 );

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

	@JiraKey(value = "HHH-9764")
	@Test
	public void testReadLockModeOnEntityLoad() {

		// first evict user
		sessionFactory().getCache().evictEntityData( User.class.getName(), 1 );

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
