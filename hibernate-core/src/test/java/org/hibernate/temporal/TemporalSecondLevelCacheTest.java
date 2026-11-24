package org.hibernate.temporal;

import jakarta.persistence.Cacheable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.hibernate.annotations.CacheLayout.SHALLOW;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.CacheSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
		TemporalSecondLevelCacheTest.TemporalThing.class,
		TemporalSecondLevelCacheTest.AuditedThing.class,
		TemporalSecondLevelCacheTest.OrdinaryThing.class,
		TemporalSecondLevelCacheTest.Article.class,
		TemporalSecondLevelCacheTest.FeatureArticle.class,
		TemporalSecondLevelCacheTest.ArticleReference.class
})
@ServiceRegistry(settings = {
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = USE_QUERY_CACHE, value = "true"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.TemporalSecondLevelCacheTest$ChangesetIds")
})
class TemporalSecondLevelCacheTest {
	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}

	@Test
	void cacheRetainsDeletedRevision(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new FeatureArticle();
			article.section = "science";
			article.id = 4;
			article.title = "first";
			session.persist( article );
		} );
		scope.inTransaction( session -> session.find( Article.class, 4 ).title = "last" );
		scope.inTransaction( session -> session.remove( session.find( Article.class, 4 ) ) );

		final var factory = scope.getSessionFactory();
		final long hits = factory.getStatistics().getQueryCacheHitCount();
		for ( int i = 0; i < 2; i++ ) {
			try ( var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession() ) {
				final long statements = factory.getStatistics().getPrepareStatementCount();
				final var rows = session.createSelectionQuery(
						"from Article a where a.id = 4 order by changesetId(a)", Article.class )
						.setCacheable( true ).getResultList();
				assertEquals( List.of( "first", "last", "last" ), rows.stream().map( a -> a.title ).toList() );
				assertNotSame( rows.get( 1 ), rows.get( 2 ) );
				assertEquals( "science", ((FeatureArticle) rows.get( 2 )).section );
				assertEquals( i == 0 ? 1 : 0, factory.getStatistics().getPrepareStatementCount() - statements );
			}
		}
		assertEquals( hits + 1, factory.getStatistics().getQueryCacheHitCount() );
	}

	@Test
	void shallowCacheReloadsEachRevision(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new Article();
			article.id = 3;
			article.title = "original";
			session.persist( article );
			final var ref = new ArticleReference();
			ref.id = 3;
			ref.article = article;
			session.persist( ref );
		} );
		scope.inTransaction( session -> session.find( Article.class, 3 ).title = "latest" );

		final var factory = scope.getSessionFactory();
		final var hits = factory.getStatistics().getQueryCacheHitCount();
		for ( int i = 0; i < 2; i++ ) {
			try ( var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession() ) {
				final var rows = session.createSelectionQuery( """
						select revision, currentArticle from Article revision cross join ArticleReference ref
						join ref.article currentArticle where revision.id = 3 and ref.id = 3
						order by changesetId(revision)
						""", Object[].class ).setCacheable( true ).getResultList();
				assertEquals( 2, rows.size() );
				final var first = (Article) rows.get( 0 )[0];
				final var second = (Article) rows.get( 1 )[0];
				final var current = (Article) rows.get( 0 )[1];
				assertEquals( "original", first.title );
				assertEquals( "latest", second.title );
				assertEquals( "latest", current.title );
				assertNotSame( first, second );
				assertNotSame( second, current );
			}
		}
		assertEquals( hits + 1, factory.getStatistics().getQueryCacheHitCount() );
	}

	@Test
	void temporal(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new TemporalThing( 1, "v1", "a" ) ) );
		final var changeset = scope.fromTransaction( session -> {
			final var thing = session.find( TemporalThing.class, 1 );
			thing.name = "v2";
			thing.tags.add( "b" );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var thing = session.find( TemporalThing.class, 1 );
			thing.name = "v3";
			thing.tags.add( "c" );
		} );

		final var cache = scope.getSessionFactory().getCache();
		final String tags = TemporalThing.class.getName() + ".tags";
		scope.inSession( session -> session.find( TemporalThing.class, 1 ).tags.size() );
		assertTrue( cache.containsEntity( TemporalThing.class, 1 ) );
		assertTrue( cache.containsCollection( tags, 1 ) );

		// a historical read must not see the current state in the cache
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var thing = session.find( TemporalThing.class, 1 );
			assertEquals( "v2", thing.name );
			assertEquals( Set.of( "a", "b" ), thing.tags );
			assertTrue( session.isReadOnly( thing ) );
		}

		// a historical read must not leave its snapshot in the cache
		cache.evictAllRegions();
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			session.find( TemporalThing.class, 1 ).tags.size();
		}
		scope.inSession( session -> {
			final var thing = session.find( TemporalThing.class, 1 );
			assertEquals( "v3", thing.name );
			assertEquals( Set.of( "a", "b", "c" ), thing.tags );
		} );
	}

	@Test
	void audited(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new AuditedThing( 1, "v1", "a" ) ) );
		final var changeset = scope.fromTransaction( session -> {
			final var thing = session.find( AuditedThing.class, 1 );
			thing.name = "v2";
			thing.tags.add( "b" );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var thing = session.find( AuditedThing.class, 1 );
			thing.name = "v3";
			thing.tags.add( "c" );
		} );

		final var cache = scope.getSessionFactory().getCache();
		final String tags = AuditedThing.class.getName() + ".tags";
		scope.inSession( session -> session.find( AuditedThing.class, 1 ).tags.size() );
		assertTrue( cache.containsEntity( AuditedThing.class, 1 ) );
		assertTrue( cache.containsCollection( tags, 1 ) );

		// a historical read must not see the current state in the cache
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var thing = session.find( AuditedThing.class, 1 );
			assertEquals( "v2", thing.name );
			assertEquals( Set.of( "a", "b" ), thing.tags );
			assertTrue( session.isReadOnly( thing ) );
		}

		// a historical read must not leave its snapshot in the cache
		cache.evictAllRegions();
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			session.find( AuditedThing.class, 1 ).tags.size();
		}
		scope.inSession( session -> {
			final var thing = session.find( AuditedThing.class, 1 );
			assertEquals( "v3", thing.name );
			assertEquals( Set.of( "a", "b", "c" ), thing.tags );
		} );
	}

	@Test
	void historicalSessionHitsNonTemporalCollectionCache(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
			final var thing = new OrdinaryThing();
			thing.id = 1;
			thing.tags.add( "a" );
			session.persist( thing );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inSession( session -> assertEquals( Set.of( "a" ), session.find( OrdinaryThing.class, 1 ).tags ) );

		final var factory = scope.getSessionFactory();
		final String role = OrdinaryThing.class.getName() + ".tags";
		assertTrue( factory.getCache().containsCollection( role, 1 ) );
		try (var session = factory.withOptions().atChangeset( changeset ).openSession()) {
			final var thing = session.find( OrdinaryThing.class, 1 );
			final var statistics = factory.getStatistics();
			statistics.clear();
			assertEquals( Set.of( "a" ), thing.tags );
			assertEquals( 1, statistics.getCollectionStatistics( role ).getCacheHitCount() );
			assertEquals( 0, statistics.getPrepareStatementCount() );
		}
	}

	@Test
	void currentArticleFetchedAlongsideOlderRevision(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new Article();
			article.id = 1;
			article.title = "Original title";
			session.persist( article );
			final var reference = new ArticleReference();
			reference.id = 1;
			reference.article = article;
			session.persist( reference );
		} );
		scope.inTransaction( session -> session.find( Article.class, 1 ).title = "Latest title" );

		final var factory = scope.getSessionFactory();
		factory.getCache().evictEntityData( Article.class, 1 );
		try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			final var row = session.createSelectionQuery( """
					select revision, ref from Article revision cross join ArticleReference ref
					join fetch ref.article
					where revision.title = 'Original title' and ref.id = 1
					""", Object[].class ).getSingleResult();
			final var revision = (Article) row[0];
			// The non-audited reference is current, so its article is too
			final var current = ( (ArticleReference) row[1] ).article;
			assertEquals( "Original title", revision.title );
			assertEquals( "Latest title", current.title );
			assertNotSame( revision, current );
		}
		// only the current state reaches the cache
		assertTrue( factory.getCache().containsEntity( Article.class, 1 ) );
		scope.inSession( session -> assertEquals( "Latest title", session.find( Article.class, 1 ).title ) );
	}

	@Test
	void currentArticleRetainsAuditExcludedNote(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var article = new Article();
			article.id = 2;
			article.title = "Draft";
			article.note = "Editorial note";
			session.persist( article );
			final var reference = new ArticleReference();
			reference.id = 2;
			reference.article = article;
			session.persist( reference );
		} );

		final var factory = scope.getSessionFactory();
		final var cache = factory.getCache();
		cache.evictEntityData( Article.class, 2 );
		try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			session.createSelectionQuery(
					"from ArticleReference ref join fetch ref.article where ref.id = 2", ArticleReference.class )
					.getSingleResult();
		}

		final var cachedNote = scope.fromTransaction( session -> {
			final var statistics = factory.getStatistics().getEntityStatistics( Article.class.getName() );
			final long hits = statistics.getCacheHitCount();
			final var article = session.find( Article.class, 2 );
			assertEquals( hits + 1, statistics.getCacheHitCount() );
			article.title = "Published";
			return article.note;
		} );

		cache.evictEntityData( Article.class, 2 );
		scope.inSession( session -> {
			final var article = session.find( Article.class, 2 );
			assertAll(
					() -> assertEquals( "Editorial note", cachedNote, "Note on cache hit" ),
					() -> assertEquals( "Published", article.title ),
					() -> assertEquals( "Editorial note", article.note, "Note after update and eviction" )
			);
		} );
	}

	@Entity(name = "Article")
	@QueryCacheLayout(layout = SHALLOW)
	@Audited
	@Cacheable
	@Cache(usage = READ_WRITE)
	static class Article {
		@Id int id;
		String title;
		@Audited.Excluded String note;
	}

	@Entity(name = "FeatureArticle")
	@Audited
	static class FeatureArticle extends Article {
		String section;
	}

	@Entity(name = "ArticleReference")
	static class ArticleReference {
		@Id int id;
		@ManyToOne(fetch = LAZY) Article article;
	}

	@Entity(name = "OrdinaryThing")
	static class OrdinaryThing {
		@Id int id;
		@ElementCollection
		@Cache(usage = READ_WRITE)
		Set<String> tags = new HashSet<>();
	}

	@Entity(name = "TemporalThing")
	@Temporal
	@Cacheable
	@Cache(usage = READ_WRITE)
	static class TemporalThing {
		@Id int id;
		String name;
		@ElementCollection
		@Cache(usage = READ_WRITE)
		Set<String> tags = new HashSet<>();

		TemporalThing() {
		}

		TemporalThing(int id, String name, String tag) {
			this.id = id;
			this.name = name;
			this.tags.add( tag );
		}
	}

	@Entity(name = "AuditedThing")
	@Audited
	@Cacheable
	@Cache(usage = READ_WRITE)
	static class AuditedThing {
		@Id int id;
		String name;
		@ElementCollection
		@Cache(usage = READ_WRITE)
		Set<String> tags = new HashSet<>();

		AuditedThing() {
		}

		AuditedThing(int id, String name, String tag) {
			this.id = id;
			this.name = name;
			this.tags.add( tag );
		}
	}
}
