package org.hibernate.temporal.audit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At a changeset, {@link Hibernate#contains}, {@link Hibernate#get} and
 * {@link Hibernate#size} of an uninitialized one-to-many collection look up
 * the rows of the element table at the changeset.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		AuditOneToManyLookupTest.Owner.class,
		AuditOneToManyLookupTest.Child.class,
		AuditOneToManyLookupTest.Entry.class
})
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditOneToManyLookupTest$ChangesetIds"))
class AuditOneToManyLookupTest {

	@Test
	void lookupsAtChangeset(SessionFactoryScope scope) {
		final Object changeset = scope.fromTransaction( session -> {
			final var owner = new Owner();
			session.persist( owner );
			final var child = new Child();
			child.owner = owner;
			session.persist( child );
			final var entry = new Entry();
			entry.code = "c";
			entry.owner = owner;
			session.persist( entry );
			return session.getCurrentChangesetIdentifier();
		} );
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1 );
			final var child = new Child();
			child.id = 2;
			child.owner = owner;
			session.persist( child );
		} );

		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
			final var owner = session.find( Owner.class, 1 );
			assertTrue( Hibernate.contains( owner.children, session.find( Child.class, 1 ) ) );
			assertEquals( 1, Hibernate.size( owner.children ) );
			assertSame( session.find( Entry.class, 1 ), Hibernate.get( owner.entries, "c" ) );
			assertNull( Hibernate.get( owner.entries, "d" ) );
			assertTrue( AuditOneToManyIndexTest.containsKey( session, Owner.class, "entries", "c" ) );
			assertFalse( AuditOneToManyIndexTest.containsKey( session, Owner.class, "entries", "d" ) );
			assertFalse( Hibernate.isInitialized( owner.children ) );
			assertFalse( Hibernate.isInitialized( owner.entries ) );
		}
	}

	@Audited
	@Entity(name = "Owner")
	static class Owner {
		@Id
		int id = 1;
		@OneToMany(mappedBy = "owner")
		Set<Child> children = new HashSet<>();
		@OneToMany(mappedBy = "owner")
		@MapKey(name = "code")
		Map<String, Entry> entries = new HashMap<>();
	}

	@Audited
	@Entity(name = "Child")
	static class Child {
		@Id
		int id = 1;
		@ManyToOne
		Owner owner;
	}

	@Audited
	@Entity(name = "Entry")
	static class Entry {
		@Id
		int id = 1;
		String code;
		@ManyToOne
		Owner owner;
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
