/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cfg.Environment;
import org.hibernate.persister.collection.CollectionPersister;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andreas Berger
 */
@JiraKey(value = "HHH-4910")
@DomainModel(
		annotatedClasses = {
				User.class, Company.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.AUTO_EVICT_COLLECTION_CACHE, value = "true"),
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
				@Setting(name = Environment.DEFAULT_LIST_SEMANTICS, value = "bag"), // CollectionClassification.BAG
		}
)
@SessionFactory(generateStatistics = true)
public class CollectionCacheEvictionTest {

	@BeforeEach
	public void before() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;
	}

	@AfterEach
	public void after() {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@BeforeEach
	void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( s -> {

			Company company1 = new Company( 1 );
			s.persist( company1 );

			User user = new User( 1, company1 );
			s.persist( user );

			Company company2 = new Company( 2 );
			s.persist( company2 );

		} );
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.getSessionFactory().getCache().evictAll();
	}

	@Test
	public void testCachedValueAfterEviction(SessionFactoryScope scope) {
		CollectionPersister persister = scope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getCollectionDescriptor( Company.class.getName() + ".users" );

		scope.inSession( session -> {
			CollectionDataAccess cache = persister.getCacheAccessStrategy();
			Object key = cache.generateCacheKey( 1, persister, session.getFactory(), session.getTenantIdentifier() );
			Object cachedValue = cache.get( session, key );
			assertNull( cachedValue );

			Company company = session.get( Company.class, 1 );
			//should add in cache
			assertEquals( 1, company.getUsers().size() );
		} );

		scope.inSession( session -> {
			CollectionDataAccess cache = persister.getCacheAccessStrategy();
			Object key = cache.generateCacheKey( 1, persister, session.getFactory(), session.getTenantIdentifier() );
			Object cachedValue = cache.get( session, key );
			assertNotNull( cachedValue, "Collection wasn't cached" );
		} );
	}

	@Test
	public void testCollectionCacheEvictionInsert(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Company company = (Company) s.get( Company.class, 1 );
			// init cache of collection
			assertEquals( 1, company.getUsers().size() );

			User user = new User( 2, company );
			s.persist( user );

		} );

		scope.inTransaction( s -> {
			Company company = (Company) s.get( Company.class, 1 );
			// fails if cache is not evicted
			assertEquals( 2, company.getUsers().size() );

		} );
	}

	@Test
	public void testCollectionCacheEvictionInsertWithEntityOutOfContext(SessionFactoryScope scope) {
		Company company = scope.fromSession( s -> {
			Company c = s.get( Company.class, 1 );
			assertEquals( 1, c.getUsers().size() );
			return c;
		} );

		scope.inTransaction( s -> {
			User user = new User( 2, company );
			s.persist( user );
		} );

		scope.inSession( s -> {
			Company c = s.get( Company.class, 1 );
			assertEquals( 2, c.getUsers().size() );
		} );
	}

	@Test
	public void testCollectionCacheEvictionRemove(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Company company = (Company) s.get( Company.class, 1 );
			// init cache of collection
			assertEquals( 1, company.getUsers().size() );

			s.remove( company.getUsers().get( 0 ) );

		} );

		scope.inTransaction( s -> {
			Company company = (Company) s.get( Company.class, 1 );
			// fails if cache is not evicted
			try {
				assertEquals( 0, company.getUsers().size() );
			}
			catch (ObjectNotFoundException e) {
				fail( "Cached element not found" );
			}
		} );
	}

	@Test
	public void testCollectionCacheEvictionRemoveWithEntityOutOfContext(SessionFactoryScope scope) {
		Company company = scope.fromSession( s -> {
			Company c = s.get( Company.class, 1 );
			assertEquals( 1, c.getUsers().size() );
			return c;
		} );

		scope.inTransaction( s -> {
			s.remove( company.getUsers().get( 0 ) );
		} );

		scope.inSession( s -> {

			var c = s.get( Company.class, 1 );
			try {
				assertEquals( 0, c.getUsers().size() );
			}
			catch (ObjectNotFoundException e) {
				fail( "Cached element not found" );
			}
		} );
	}

	@Test
	public void testCollectionCacheEvictionUpdate(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Company company1 = (Company) s.get( Company.class, 1 );
			Company company2 = (Company) s.get( Company.class, 2 );

			// init cache of collection
			assertEquals( 1, company1.getUsers().size() );
			assertEquals( 0, company2.getUsers().size() );

			User user = (User) s.get( User.class, 1 );
			user.setCompany( company2 );
		} );

		scope.inSession( s -> {
			var company1 = (Company) s.get( Company.class, 1 );
			var company2 = (Company) s.get( Company.class, 2 );

			assertEquals( 1, company2.getUsers().size() );

			// fails if cache is not evicted
			try {
				assertEquals( 0, company1.getUsers().size() );
			}
			catch (ObjectNotFoundException e) {
				fail( "Cached element not found" );
			}

		} );
	}

	@Test
	@JiraKey(value = "HHH-10631")
	public void testCollectionCacheEvictionUpdateWhenChildIsSetToNull(SessionFactoryScope scope) {
		scope.inTransaction( s -> {

			Company company1 = (Company) s.get( Company.class, 1 );
			Company company2 = (Company) s.get( Company.class, 2 );

			// init cache of collection
			assertEquals( 1, company1.getUsers().size() );
			assertEquals( 0, company2.getUsers().size() );

			User user = (User) s.get( User.class, 1 );
			user.setCompany( null );
		} );

		scope.inSession( s -> {
			var company1 = (Company) s.get( Company.class, 1 );
			var company2 = (Company) s.get( Company.class, 2 );

			assertEquals( 0, company1.getUsers().size() );
			assertEquals( 0, company2.getUsers().size() );
		} );
	}

	@Test
	public void testCollectionCacheEvictionUpdateWithEntityOutOfContext(SessionFactoryScope scope) {
		Company c = scope.fromSession( s -> {
			Company company1 = s.get( Company.class, 1 );
			Company company2 = s.get( Company.class, 2 );

			assertEquals( 1, company1.getUsers().size() );
			assertEquals( 0, company2.getUsers().size() );
			return company2;
		} );
		scope.inTransaction( s -> {
			User user = s.get( User.class, 1 );
			user.setCompany( c );
		} );

		scope.inSession( s -> {

			var company1 = s.get( Company.class, 1 );
			var company2 = s.get( Company.class, 2 );

			assertEquals( 1, company2.getUsers().size() );

			try {
				assertEquals( 0, company1.getUsers().size() );
			}
			catch (ObjectNotFoundException e) {
				fail( "Cached element not found" );
			}
		} );
	}

	@Test
	public void testUpdateWithNullRelation(SessionFactoryScope scope) {
		User user = scope.fromTransaction( session -> {
			User u = new User();
			u.setName( "User1" );
			session.persist( u );
			return u;
		} );

		scope.inTransaction( session -> {
			user.setName( "UserUpdate" );
			session.merge( user );
		} );
	}
}
