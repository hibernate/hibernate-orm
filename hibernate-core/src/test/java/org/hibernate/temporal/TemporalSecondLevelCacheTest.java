package org.hibernate.temporal;

import jakarta.persistence.Cacheable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Temporal;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
		TemporalSecondLevelCacheTest.TemporalThing.class,
		TemporalSecondLevelCacheTest.AuditedThing.class,
		TemporalSecondLevelCacheTest.OrdinaryThing.class
})
@ServiceRegistry(settings = {
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true"),
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
