/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.orm.test.entitygraph.EntityGraphCacheStoreModeTest_.Book_;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedEntityGraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;

@DomainModel(
		annotatedClasses = {
				EntityGraphCacheStoreModeTest.Book.class,
				EntityGraphCacheStoreModeTest.Author.class,
				EntityGraphCacheStoreModeTest.Publisher.class
		}
)
@ServiceRegistry(
		settings = @Setting(name = USE_SECOND_LEVEL_CACHE, value = "true")
)
@SessionFactory(generateStatistics = true)
class EntityGraphCacheStoreModeTest {
	private static final String NAMED_GRAPH = "Book.with-cache-store-modes";
	private static final String BYPASS_TAGS_ROLE = EntityGraphCacheStoreModeTest.Book.class.getName() + ".bypassTags";
	private static final String USE_TAGS_ROLE = EntityGraphCacheStoreModeTest.Book.class.getName() + ".useTags";

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var author = new Author( 1L );
			final var publisher = new Publisher( 1L );
			session.persist( author );
			session.persist( publisher );
			session.persist( new Book( 1L, author, publisher ) );
		} );
		final var sessionFactory = scope.getSessionFactory();
		sessionFactory.getCache().evictAllRegions();
		sessionFactory.getStatistics().clear();
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		final var sessionFactory = scope.getSessionFactory();
		sessionFactory.getSchemaManager().truncate();
		sessionFactory.getCache().evictAllRegions();
		sessionFactory.getStatistics().clear();
	}

	@Test
	void programmaticGraphCacheStoreModeControlsAssociationPuts(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author ).addOption( CacheStoreMode.BYPASS );
			graph.addAttributeNode( Book_.publisher ).addOption( CacheStoreMode.USE );
			graph.addAttributeNode( Book_.bypassTags ).addOption( CacheStoreMode.BYPASS );
			graph.addAttributeNode( Book_.useTags ).addOption( CacheStoreMode.USE );

			final var book =
					session.createQuery( "from GraphCacheStoreBook where id = 1", Book.class )
							.setEntityGraph( graph, GraphSemantic.FETCH )
							.getSingleResult();

			assertThat( isInitialized( book.author ) ).isTrue();
			assertThat( isInitialized( book.publisher ) ).isTrue();
			assertThat( isInitialized( book.bypassTags ) ).isTrue();
			assertThat( isInitialized( book.useTags ) ).isTrue();
		} );

		final var cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Author.class, 1L ) ).isFalse();
		assertThat( cache.containsEntity( Publisher.class, 1L ) ).isTrue();
		assertThat( cache.containsCollection( BYPASS_TAGS_ROLE, 1L ) ).isFalse();
		assertThat( cache.containsCollection( USE_TAGS_ROLE, 1L ) ).isTrue();
	}

	@Test
	void fetchAnnotationCacheStoreModeControlsAssociationPuts(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book =
					session.createQuery( "from GraphCacheStoreBook where id = 1", Book.class )
							.setEntityGraph( Book_._Book_with_cache_store_modes, GraphSemantic.FETCH )
							.getSingleResult();

			assertThat( isInitialized( book.author ) ).isTrue();
			assertThat( isInitialized( book.publisher ) ).isTrue();
			assertThat( isInitialized( book.bypassTags ) ).isTrue();
			assertThat( isInitialized( book.useTags ) ).isTrue();
		} );

		final var cache = scope.getSessionFactory().getCache();
		assertThat( cache.containsEntity( Author.class, 1L ) ).isFalse();
		assertThat( cache.containsEntity( Publisher.class, 1L ) ).isTrue();
		assertThat( cache.containsCollection( BYPASS_TAGS_ROLE, 1L ) ).isFalse();
		assertThat( cache.containsCollection( USE_TAGS_ROLE, 1L ) ).isTrue();
	}

	@Entity(name = "GraphCacheStoreBook")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@NamedEntityGraph(name = NAMED_GRAPH)
	static class Book {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheStoreMode = CacheStoreMode.BYPASS
		)
		private Author author;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheStoreMode = CacheStoreMode.USE
		)
		private Publisher publisher;

		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "GraphCacheStoreBook_bypassTags")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheStoreMode = CacheStoreMode.BYPASS
		)
		private Set<String> bypassTags = new HashSet<>();

		@ElementCollection(fetch = FetchType.LAZY)
		@CollectionTable(name = "GraphCacheStoreBook_useTags")
		@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheStoreMode = CacheStoreMode.USE
		)
		private Set<String> useTags = new HashSet<>();

		Book() {
		}

		Book(Long id, Author author, Publisher publisher) {
			this.id = id;
			this.author = author;
			this.publisher = publisher;
			bypassTags.add( "bypass" );
			useTags.add( "use" );
		}
	}

	@Entity(name = "GraphCacheStoreAuthor")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Author {
		@Id
		private Long id;

		Author() {
		}

		Author(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "GraphCacheStorePublisher")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Publisher {
		@Id
		private Long id;

		Publisher() {
		}

		Publisher(Long id) {
			this.id = id;
		}
	}
}
