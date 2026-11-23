package org.hibernate.temporal.audit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyJoinColumn;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.annotations.Cache;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At a changeset, the audited keys of a map of an entity without history are read at
 * the changeset, whether or not the map is in the second-level cache: a key has its
 * state at the changeset, and the entry for a key which did not exist yet is absent.
 */
@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = { AuditMapKeyHistoryTest.Holder.class, AuditMapKeyHistoryTest.Tag.class })
@ServiceRegistry(settings = {
		@Setting(name = USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.AuditMapKeyHistoryTest$ChangesetIds")
})
class AuditMapKeyHistoryTest {

	private static final String ROLE = Holder.class.getName() + ".entries";

	@Test
	void keyAtChangeset(SessionFactoryScope scope) {
		final Object first = scope.fromTransaction( session -> {
			final var tag = new Tag();
			tag.id = 1;
			tag.label = "old";
			session.persist( tag );
			final var holder = new Holder();
			holder.id = 1;
			holder.entries.put( tag, "one" );
			session.persist( holder );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> session.find( Tag.class, 1 ).label = "new" );

		for ( boolean cached : List.of( true, false ) ) {
			cacheOrEvict( scope, 1, cached );
			try (var session = scope.getSessionFactory().withOptions().atChangeset( first ).openSession()) {
				final var entry = session.find( Holder.class, 1 ).entries.entrySet().iterator().next();
				assertEquals( "old", ( (Tag) Hibernate.unproxy( entry.getKey() ) ).label );
				assertEquals( "one", entry.getValue() );
			}
		}
	}

	@Test
	void keyAddedAfterChangeset(SessionFactoryScope scope) {
		final Object first = scope.fromTransaction( session -> {
			final var tag = new Tag();
			tag.id = 2;
			tag.label = "first";
			session.persist( tag );
			final var holder = new Holder();
			holder.id = 2;
			holder.entries.put( tag, "early" );
			session.persist( holder );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var tag = new Tag();
			tag.id = 3;
			tag.label = "second";
			session.persist( tag );
			session.find( Holder.class, 2 ).entries.put( tag, "late" );
		} );

		for ( boolean cached : List.of( true, false ) ) {
			cacheOrEvict( scope, 2, cached );
			try (var session = scope.getSessionFactory().withOptions().atChangeset( first ).openSession()) {
				final var entries = session.find( Holder.class, 2 ).entries;
				assertEquals( "early", Hibernate.get( entries, session.find( Tag.class, 2 ) ) );
				assertNull( Hibernate.get( entries, session.getReference( Tag.class, 3 ) ) );
				assertEquals( List.of( "early" ), List.copyOf( new TreeSet<>( entries.values() ) ) );
			}
		}
	}

	private static void cacheOrEvict(SessionFactoryScope scope, int holder, boolean cached) {
		final var cache = scope.getSessionFactory().getCache();
		cache.evictAllRegions();
		if ( cached ) {
			// the cache holds the current state of the map
			scope.inSession( session -> Hibernate.initialize( session.find( Holder.class, holder ).entries ) );
			assertTrue( cache.containsCollection( ROLE, holder ) );
		}
	}

	@Entity(name = "Holder")
	static class Holder {
		@Id
		int id;
		@ElementCollection
		@MapKeyJoinColumn(name = "tag_id")
		@Column(name = "entry_text")
		@Cache(usage = READ_WRITE)
		Map<Tag, String> entries = new HashMap<>();
	}

	@Audited
	@Entity(name = "Tag")
	static class Tag {
		@Id
		int id;
		String label;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
