/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import java.io.Serializable;

import org.hibernate.CacheMode;
import org.hibernate.cache.internal.BasicCacheKeyImplementation;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cache.spi.entry.CollectionCacheEntry;
import org.hibernate.cache.spi.support.AbstractReadWriteAccess;
import org.hibernate.cache.spi.support.CollectionReadWriteAccess;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.model.domain.NavigableRole;

import org.hibernate.testing.cache.MapStorageAccessImpl;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.domain.library.Person;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.SharedCacheMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Assertions that collections which are join fetched and restricted in a query do not get put into the
 * second level cache with the filtered state
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jira( "https://hibernate.atlassian.net/browse/HHH-2003" )
@ServiceRegistry(settings = {
		@Setting( name= AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting( name= AvailableSettings.USE_QUERY_CACHE, value = "true"),
})
@DomainModel(standardModels = StandardDomainModel.LIBRARY, sharedCacheMode = SharedCacheMode.ALL)
@SessionFactory
public class QueryRestrictedCollectionCachingTests {
	public static final String AUTHORS_ROLE = Book.class.getName() + ".authors";

	@Test
	void testSimpleFetch(SessionFactoryScope sessions) {
		final CacheImplementor cache = sessions.getSessionFactory().getCache();
		cache.evictAllRegions();

		sessions.inTransaction( (session) -> {
			final Book book = session.createSelectionQuery( "from Book b left join fetch b.authors a", Book.class ).getSingleResult();
			assertThat( book ).isNotNull();
			assertThat( book.getAuthors() ).hasSize( 2 );
		} );

		assertThat( cache.containsCollection( AUTHORS_ROLE, 1 ) ).isTrue();
		assertThat( extractCachedCollectionKeys( cache, AUTHORS_ROLE, 1 ) ).hasSize( 2 );
	}

	@Test
	void testSimpleFetch2(SessionFactoryScope sessions) {
		final CacheImplementor cache = sessions.getSessionFactory().getCache();
		cache.evictAllRegions();

		sessions.inTransaction( (session) -> {
			final Book book = session.createSelectionQuery(
					"from Book b left join fetch b.authors a where b.id = 1",
					Book.class
			).getSingleResult();
			assertThat( book ).isNotNull();
			assertThat( book.getAuthors() ).hasSize( 2 );
		} );

		assertThat( cache.containsCollection( AUTHORS_ROLE, 1 ) ).isTrue();
		assertThat( extractCachedCollectionKeys( cache, AUTHORS_ROLE, 1 ) ).hasSize( 2 );
	}

	@Test
	void testRestrictedFetchWithCacheIgnored(SessionFactoryScope sessions) {
		final CacheImplementor cache = sessions.getSessionFactory().getCache();
		cache.evictAllRegions();

		sessions.inTransaction( (session) -> {
			final Book book = session
					.createSelectionQuery( "from Book b left join fetch b.authors a where a.id = 1", Book.class )
					.setCacheMode( CacheMode.IGNORE )
					.getSingleResult();
			assertThat( book ).isNotNull();
			assertThat( book.getAuthors() ).hasSize( 1 );
		} );

		// we ignored the cache explicitly
		assertThat( cache.containsCollection( AUTHORS_ROLE, 1 ) ).isFalse();
	}

	@Test
	@FailureExpected
	void testRestrictedFetch(SessionFactoryScope sessions) {
		final CacheImplementor cache = sessions.getSessionFactory().getCache();
		cache.evictAllRegions();

		sessions.inTransaction( (session) -> {
			final Book book = session
					.createSelectionQuery( "from Book b left join fetch b.authors a where a.id = 1", Book.class )
					.getSingleResult();
			assertThat( book ).isNotNull();
			assertThat( book.getAuthors() ).hasSize( 1 );
		} );

		// This is the crux of HHH-2003.
		// At the moment we put the filtered collection into the cache
		assertThat( cache.containsCollection( AUTHORS_ROLE, 1 ) ).isTrue();
		// this is just some deeper checks to show that the data is "corrupt"
		assertThat( extractCachedCollectionKeys( cache, AUTHORS_ROLE, 1 ) ).hasSize( 1 );

		fail( "Really, HHH-2003 the collection to not be cached here" );
	}

	private static Serializable[] extractCachedCollectionKeys(CacheImplementor cache, String role, Integer ownerKey) {
		final NavigableRole navigableRole = new NavigableRole( role );
		final CollectionReadWriteAccess authorsRegionAccess = (CollectionReadWriteAccess) cache.getCollectionRegionAccess( navigableRole );

		final MapStorageAccessImpl storageAccess = (MapStorageAccessImpl) authorsRegionAccess.getStorageAccess();
		final BasicCacheKeyImplementation cacheKey = new BasicCacheKeyImplementation( ownerKey, role, ownerKey );
		final AbstractReadWriteAccess.Item cacheItem = (AbstractReadWriteAccess.Item) storageAccess.getFromData( cacheKey );
		assertThat( cacheItem ).isNotNull();

		final CollectionCacheEntry cacheEntry = (CollectionCacheEntry) cacheItem.getValue();
		return cacheEntry.getState();
	}

	@BeforeEach
	void createTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Person poe = new Person( 1, "John Poe" );
			session.persist( poe );

			final Person schmidt = new Person( 2, "Jacob Schmidt" );
			session.persist( schmidt );

			final Person king = new Person( 3, "David King" );
			session.persist( king );

			final Book nightsEdge = new Book( 1, "A Night's Edge" );
			nightsEdge.addAuthor( poe );
			nightsEdge.addAuthor( king );
			nightsEdge.addEditor( schmidt );
			session.persist( nightsEdge );
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}
}
