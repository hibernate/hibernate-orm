/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.orm.test.entitygraph.EntityGraphCacheRetrieveModeTest_.Book_;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;

@DomainModel(
		annotatedClasses = {
				EntityGraphCacheRetrieveModeTest.Book.class,
				EntityGraphCacheRetrieveModeTest.Author.class,
				EntityGraphCacheRetrieveModeTest.Publisher.class,
				EntityGraphCacheRetrieveModeTest.Tag.class
		}
)
@ServiceRegistry(
		settings = @Setting(name = USE_SECOND_LEVEL_CACHE, value = "true")
)
@SessionFactory(generateStatistics = true)
class EntityGraphCacheRetrieveModeTest {
	private static final String NAMED_GRAPH = "Book.with-cache-retrieve-modes";

	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var author = new Author( 1L, "cached author" );
			final var publisher = new Publisher( 1L, "cached publisher" );
			final var bypassTag = new Tag( 1L, "cached bypass tag" );
			final var useTag = new Tag( 2L, "cached use tag" );
			session.persist( author );
			session.persist( publisher );
			session.persist( bypassTag );
			session.persist( useTag );
			session.persist( new Book( 1L, author, publisher, bypassTag, useTag ) );
		} );

		final var sessionFactory = scope.getSessionFactory();
		final var cache = sessionFactory.getCache();
		cache.evictAllRegions();

		scope.inTransaction( session -> {
			assertThat( session.get( Author.class, 1L ).name ).isEqualTo( "cached author" );
			assertThat( session.get( Publisher.class, 1L ).name ).isEqualTo( "cached publisher" );
			assertThat( session.get( Tag.class, 1L ).name ).isEqualTo( "cached bypass tag" );
			assertThat( session.get( Tag.class, 2L ).name ).isEqualTo( "cached use tag" );
		} );

		assertThat( cache.containsEntity( Author.class, 1L ) ).isTrue();
		assertThat( cache.containsEntity( Publisher.class, 1L ) ).isTrue();
		assertThat( cache.containsEntity( Tag.class, 1L ) ).isTrue();
		assertThat( cache.containsEntity( Tag.class, 2L ) ).isTrue();

		scope.inTransaction( session -> session.doWork( connection -> {
			updateName( connection, "GraphCacheRetrieveAuthor", 1L, "database author" );
			updateName( connection, "GraphCacheRetrievePublisher", 1L, "database publisher" );
			updateName( connection, "GraphCacheRetrieveTag", 1L, "database bypass tag" );
			updateName( connection, "GraphCacheRetrieveTag", 2L, "database use tag" );
		} ) );

		cache.evictEntityData( Book.class );
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
	void programmaticGraphCacheRetrieveModeControlsAssociationGets(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setCacheRetrieveMode( CacheRetrieveMode.BYPASS );

			final var graph = session.createEntityGraph( Book.class );
			graph.addAttributeNode( Book_.author ).addOption( CacheRetrieveMode.BYPASS );
			graph.addAttributeNode( Book_.publisher ).addOption( CacheRetrieveMode.USE );
			graph.addAttributeNode( Book_.bypassTags ).addOption( CacheRetrieveMode.BYPASS );
			graph.addAttributeNode( Book_.useTags ).addOption( CacheRetrieveMode.USE );

			final var book =
					session.createQuery( "from GraphCacheRetrieveBook where id = 1", Book.class )
							.setEntityGraph( graph, GraphSemantic.FETCH )
							.getSingleResult();

			assertFetchedState( book );
		} );
	}

	@Test
	void fetchAnnotationCacheRetrieveModeControlsAssociationGets(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setCacheRetrieveMode( CacheRetrieveMode.BYPASS );

			final var book =
					session.createQuery( "from GraphCacheRetrieveBook where id = 1", Book.class )
							.setEntityGraph( Book_._Book_with_cache_retrieve_modes, GraphSemantic.FETCH )
							.getSingleResult();

			assertFetchedState( book );
		} );
	}

	private static void assertFetchedState(Book book) {
		assertThat( isInitialized( book.author ) ).isTrue();
		assertThat( isInitialized( book.publisher ) ).isTrue();
		assertThat( isInitialized( book.bypassTags ) ).isTrue();
		assertThat( isInitialized( book.useTags ) ).isTrue();

		assertThat( book.author.name ).isEqualTo( "database author" );
		assertThat( book.publisher.name ).isEqualTo( "cached publisher" );
		assertThat( book.bypassTags ).extracting( tag -> tag.name ).containsExactly( "database bypass tag" );
		assertThat( book.useTags ).extracting( tag -> tag.name ).containsExactly( "cached use tag" );
	}

	private static void updateName(java.sql.Connection connection, String table, Long id, String name)
			throws SQLException {
		try ( var statement = connection.prepareStatement( "update " + table + " set name = ? where id = ?" ) ) {
			statement.setString( 1, name );
			statement.setLong( 2, id );
			statement.executeUpdate();
		}
	}

	@Entity(name = "GraphCacheRetrieveBook")
	@Table(name = "GraphCacheRetrieveBook")
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
				cacheRetrieveMode = CacheRetrieveMode.BYPASS
		)
		private Author author;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheRetrieveMode = CacheRetrieveMode.USE
		)
		private Publisher publisher;

		@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinTable(name = "GraphCacheRetrieveBook_bypassTags")
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheRetrieveMode = CacheRetrieveMode.BYPASS
		)
		private Set<Tag> bypassTags = new HashSet<>();

		@ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@JoinTable(name = "GraphCacheRetrieveBook_useTags")
		@Fetch(
				graph = NAMED_GRAPH,
				type = FetchType.EAGER,
				cacheRetrieveMode = CacheRetrieveMode.USE
		)
		private Set<Tag> useTags = new HashSet<>();

		Book() {
		}

		Book(Long id, Author author, Publisher publisher, Tag bypassTag, Tag useTag) {
			this.id = id;
			this.author = author;
			this.publisher = publisher;
			bypassTags.add( bypassTag );
			useTags.add( useTag );
		}
	}

	@Entity(name = "GraphCacheRetrieveAuthor")
	@Table(name = "GraphCacheRetrieveAuthor")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Author {
		@Id
		private Long id;

		private String name;

		Author() {
		}

		Author(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "GraphCacheRetrievePublisher")
	@Table(name = "GraphCacheRetrievePublisher")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Publisher {
		@Id
		private Long id;

		private String name;

		Publisher() {
		}

		Publisher(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "GraphCacheRetrieveTag")
	@Table(name = "GraphCacheRetrieveTag")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	static class Tag {
		@Id
		private Long id;

		private String name;

		Tag() {
		}

		Tag(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
