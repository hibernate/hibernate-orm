package org.hibernate.temporal.audit;

import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.KeyType.NATURAL;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * In an all-changesets session, a lookup by id or natural id of an audited entity
 * reads current state, so the second-level cache, which holds current state, serves
 * it, as in a session without a changeset.
 */
@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
		AuditAllChangesetsCacheTest.Book.class,
		AuditAllChangesetsCacheTest.Shelf.class,
		AuditAllChangesetsCacheTest.Loan.class
})
@ServiceRegistry(settings = {
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.AuditAllChangesetsCacheTest$ChangesetIds")
})
class AuditAllChangesetsCacheTest {

	@Test
	void lookupsByKeyUseCache(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book();
			book.isbn = "ISBN";
			book.title = "first";
			session.persist( book );
		} );
		scope.inTransaction( session -> session.find( Book.class, 1 ).title = "second" );
		// put the resolution of the natural id in the cache
		scope.inSession( session -> session.bySimpleNaturalId( Book.class ).load( "ISBN" ) );

		final var statistics = scope.getSessionFactory().getStatistics();
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			statistics.clear();
			assertEquals( "second", session.find( Book.class, 1 ).title );
			assertEquals( 1, statistics.getSecondLevelCacheHitCount() );
			assertEquals( 0, statistics.getPrepareStatementCount() );
		}
		try (var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			statistics.clear();
			assertEquals( "second", session.bySimpleNaturalId( Book.class ).load( "ISBN" ).title );
			assertEquals( 1, statistics.getNaturalIdCacheHitCount() );
			assertEquals( 0, statistics.getPrepareStatementCount() );
			// the revisions are still read from the audit table
			assertEquals( List.of( "first", "second" ), session.createSelectionQuery(
					"select b.title from Book b where b.id = 1 order by changesetId(b)", String.class ).list() );
		}
	}

	@Test
	void lookupsByNaturalKeyUseCache(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book();
			book.id = 2;
			book.isbn = "OTHER";
			book.title = "other";
			session.persist( book );
		} );
		// put the resolution of the natural id in the cache
		scope.inSession( session -> session.bySimpleNaturalId( Book.class ).load( "OTHER" ) );

		final var factory = scope.getSessionFactory();
		final var statistics = factory.getStatistics();
		for ( boolean reference : List.of( false, true ) ) {
			try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
				statistics.clear();
				assertNotNull( reference
						? session.getReference( Book.class, "OTHER", NATURAL )
						: session.find( Book.class, "OTHER", NATURAL ) );
				assertEquals( 1, statistics.getNaturalIdCacheHitCount() );
				assertEquals( 0, statistics.getPrepareStatementCount() );
			}
		}
		// a resolution read from the database is current state, so it is cached
		factory.getCache().evictNaturalIdData( Book.class );
		try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			statistics.clear();
			assertNotNull( session.getReference( Book.class, "OTHER", NATURAL ) );
			assertEquals( 1, statistics.getNaturalIdCachePutCount() );
		}
	}

	@Test
	void currentLoadAfterSnapshotCachesNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book();
			book.id = 3;
			book.isbn = "STABLE";
			book.title = "initial";
			session.persist( book );
			final var shelf = new Shelf();
			shelf.book = book;
			session.persist( shelf );
		} );
		scope.inTransaction( session -> session.find( Book.class, 3 ).title = "revised" );

		final var factory = scope.getSessionFactory();
		final var statistics = factory.getStatistics();
		factory.getCache().evictAllRegions();
		try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// a snapshot with the natural id of the current version, read first
			assertEquals( "initial", session.createSelectionQuery( "from Book where title = 'initial'", Book.class )
					.getSingleResult().title );
			assertEquals( "revised", session.createSelectionQuery(
					"select s from Shelf s join fetch s.book where s.id = 1", Shelf.class )
					.getSingleResult().book.title );
		}
		statistics.clear();
		scope.inSession( session -> assertEquals( "revised", session.find( Book.class, "STABLE", NATURAL ).title ) );
		assertEquals( 1, statistics.getNaturalIdCacheHitCount() );
	}

	@Test
	void currentLoadAfterSnapshotProxyCachesNaturalId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var book = new Book();
			book.id = 4;
			book.isbn = "PROXIED";
			book.title = "earlier";
			session.persist( book );
			final var loan = new Loan();
			loan.note = "first";
			loan.book = book;
			session.persist( loan );
			final var shelf = new Shelf();
			shelf.id = 2;
			shelf.book = book;
			session.persist( shelf );
		} );
		scope.inTransaction( session -> {
			session.find( Book.class, 4 ).title = "later";
			session.find( Loan.class, 1 ).note = "second";
		} );

		final var factory = scope.getSessionFactory();
		final var statistics = factory.getStatistics();
		factory.getCache().evictAllRegions();
		try (var session = factory.withOptions().atChangeset( ALL_CHANGESETS ).openSession()) {
			// a snapshot with the natural id of the current version, read through an association
			final var book = session.createSelectionQuery( "from Loan where note = 'first'", Loan.class )
					.getSingleResult().book;
			assertFalse( Hibernate.isInitialized( book ) );
			assertEquals( "earlier", ( (Book) Hibernate.unproxy( book ) ).title );
			assertEquals( "later", session.createSelectionQuery(
					"select s from Shelf s join fetch s.book where s.id = 2", Shelf.class )
					.getSingleResult().book.title );
		}
		statistics.clear();
		scope.inSession( session -> assertEquals( "later", session.find( Book.class, "PROXIED", NATURAL ).title ) );
		assertEquals( 1, statistics.getNaturalIdCacheHitCount() );
	}

	@Audited
	@Entity(name = "Loan")
	static class Loan {
		@Id
		int id = 1;
		String note;
		@ManyToOne(fetch = LAZY)
		Book book;
	}

	@Entity(name = "Shelf")
	static class Shelf {
		@Id
		int id = 1;
		@ManyToOne
		Book book;
	}

	@Audited
	@Entity(name = "Book")
	@Cacheable
	@Cache(usage = READ_WRITE)
	@NaturalIdCache
	static class Book {
		@Id
		int id = 1;
		@NaturalId
		String isbn;
		String title;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
