/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.polymorphism;

import org.hibernate.WrongClassException;
import org.hibernate.cache.spi.CacheImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Guillaume Smet
 * @author Brett Meyer
 */
@JiraKey( "HHH-9028" )
@JiraKey( "HHH-9107" )
@DomainModel( annotatedClasses = { Cacheable.class, CachedItem1.class, CachedItem2.class, CacheHolder.class } )
@SessionFactory
public class PolymorphicCacheTest {
	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new CachedItem1( 1, "name 1" ) );
			session.persist( new CachedItem2( 2, "name 2" ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAll();
	}

	@Test
	public void testPolymorphismAndCache(SessionFactoryScope scope) {
		final CacheImplementor cache = scope.getSessionFactory().getCache();

		assertThat( cache.containsEntity( Cacheable.class, 1 ) ).isTrue();
		assertThat( cache.containsEntity( Cacheable.class, 2 ) ).isTrue();

		assertThat( cache.containsEntity( CachedItem1.class, 1 ) ).isTrue();
		assertThat( cache.containsEntity( CachedItem2.class, 2 ) ).isTrue();

		// would be nice-to-have
		//assertThat( cache.containsEntity( CachedItem2.class, 1 ) ).isFalse();

		// test accessing the wrong class by id with a cache-hit
		scope.inTransaction( (session) -> {
			try {
				final CachedItem2 loaded = session.get( CachedItem2.class, 1 );
				assertThat( loaded ).isNull();
			}
			catch (WrongClassException legacyBehavior) {
				// the legacy behavior for loading an entity from a
				// cache hit when it is the wrong class was to throw
				// a WrongClassException.
				//
				// this was inconsistent with cases where there is no
				// cache hit - there we return null.
				//
				// 6.0 makes the behavior here consistent by always
				// returning null in such cases
				//
				// make sure WrongClassException is not thrown here
				Assertions.fail( "WrongClassException was thrown for but returning null was expected" );
			}
		} );

		// test accessing the wrong class by id with no cache-hit
		cache.evictEntityData( Cacheable.class, 1 );
		cache.evictEntityData( Cacheable.class, 2 );
		scope.inTransaction( (session) -> {
			// the legacy behavior for no cache hit was to return null
			try {
				final CachedItem2 loaded = session.get( CachedItem2.class, 1 );
				assertThat( loaded ).isNull();
			}
			catch (WrongClassException legacyBehavior) {
				// as in the cache hit assertions above,  make sure
				// WrongClassException is not thrown here
				Assertions.fail( "WrongClassException was thrown for but returning null was expected" );
			}
		} );

		// reload into cache
		scope.inTransaction( (session) -> {
			session.get( CachedItem1.class, 1 );
			session.get( CachedItem2.class, 2 );
		} );

		assertThat( cache.containsEntity( CachedItem1.class, 1 ) ).isTrue();
		assertThat( cache.containsEntity( CachedItem2.class, 2 ) ).isTrue();

		final CachedItem1 detachedItem1 = scope.fromTransaction( (session) -> session.get( CachedItem1.class, 1 ) );

		// test updating
		scope.inSession( (session) -> {
			scope.inTransaction( session, (s) -> {
				detachedItem1.setName( "updated" );
				session.merge( detachedItem1 );
			} );

			assertThat( cache.containsEntity( CachedItem1.class, 1 ) ).isTrue();
			assertThat( cache.containsEntity( CachedItem2.class, 2 ) ).isTrue();

			scope.inTransaction( session, (s) -> {
				final CachedItem1 loadedItem1 = s.get( CachedItem1.class, 1 );
				assertThat( loadedItem1 ).isNotNull();
				assertThat( loadedItem1.getName() ).isEqualTo( "updated" );

				final CachedItem2 loadedItem2 = s.get( CachedItem2.class, 2 );
				assertThat( loadedItem2 ).isNotNull();

			} );
		} );

		// test deleting
		scope.inSession( (session) -> {
			scope.inTransaction( session, (s) -> {
				session.remove( session.getReference( CachedItem1.class, 1 ) );
			} );
			scope.inTransaction( session, (s) -> {
				final CachedItem1 cachedItem1 = session.get( CachedItem1.class, 1 );
				assertThat( cachedItem1 ).isNull();

				final CachedItem2 cachedItem2 = session.get( CachedItem2.class, 2 );
				assertThat( cachedItem2 ).isNotNull();
			} );
		} );
	}

	@Test
	@JiraKey("HHH-10162")
	public void testPolymorphismAndCacheWithHolder(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					final CachedItem1 item3 = new CachedItem1(3, "name3");
					final CacheHolder holder = new CacheHolder( "holder", item3 );
					s.persist( item3 );
					s.persist( holder );
				}
		);

		scope.inTransaction(
				s -> {
					CacheHolder cacheHolder = s.get( CacheHolder.class, "holder" );
					Assertions.assertTrue(
							cacheHolder.getItem() instanceof CachedItem1,
							"Relation was not fetched from L2 cache"
					);
				}
		);
	}

}
