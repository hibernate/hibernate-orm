/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.LockMode;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Guenther Demetz
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				User.class, Company.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.AUTO_EVICT_COLLECTION_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.DEFAULT_LIST_SEMANTICS, value = "bag"), // CollectionClassification.BAG
		}
)
@SessionFactory
public class LockModeTest {

	@BeforeEach
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@AfterEach
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
					Company company1 = new Company( 1 );
					s.persist( company1 );

					User user = new User( 1, company1 );
					s.persist( user );

					Company company2 = new Company( 2 );
					s.persist( company2 );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) {
		scope.dropData();
		scope.getSessionFactory().getCache().evictAll();
	}

	/**
	 *
	 */
	@JiraKey(value = "HHH-9764")
	@Test
	public void testDefaultLockModeOnCollectionInitialization(SessionFactoryScope scope) {
		scope.inTransaction( s1 -> {

			Company company1 = s1.find( Company.class, 1 );

			s1.find( User.class, 1 ); // into persistent context

			/******************************************
			 *
			 */
			scope.inTransaction( s2 -> {
				User user = s2.find( User.class, 1 );
				user.setName( "TestUser" );
			} );

			/******************************************
			 *
			 */

			// init cache of collection
			assertEquals( 1, company1.getUsers()
					.size() ); // raises org.hibernate.StaleObjectStateException if 2LCache is enabled
		} );
	}

	@JiraKey(value = "HHH-9764")
	@Test
	public void testDefaultLockModeOnEntityLoad(SessionFactoryScope scope) {

		// first evict user
		scope.getSessionFactory().getCache().evictEntityData( User.class.getName(), 1 );

		scope.inTransaction( s1 -> {

			s1.find( Company.class, 1 );

			/******************************************
			 *
			 */
			scope.inTransaction( s2 -> {
				Company company = s2.find( Company.class, 1 );
				company.setName( "TestCompany" );
			} );


			/******************************************
			 *
			 */

			User user1 = s1.find( User.class, 1 ); // into persistent context

			// init cache of collection
			assertNull(
					user1.getCompany()
							.getName() ); // raises org.hibernate.StaleObjectStateException if 2LCache is enabled

		} );
	}

	@JiraKey(value = "HHH-9764")
	@Test
	public void testReadLockModeOnEntityLoad(SessionFactoryScope scope) {

		// first evict user
		scope.getSessionFactory().getCache().evictEntityData( User.class.getName(), 1 );

		scope.inTransaction( s1 -> {

			s1.find( Company.class, 1 );

			/******************************************
			 *
			 */
			scope.inTransaction( s2 -> {
				Company company = s2.find( Company.class, 1 );
				company.setName( "TestCompany" );
			} );


			/******************************************
			 *
			 */

			User user1 = s1.find( User.class, 1, LockMode.READ ); // into persistent context

			// init cache of collection
			assertNull(
					user1.getCompany()
							.getName() ); // raises org.hibernate.StaleObjectStateException if 2LCache is enabled

		} );
	}

}
