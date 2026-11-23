package org.hibernate.temporal.audit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyJoinColumn;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At a changeset, {@link Hibernate#contains} and {@link Hibernate#get} of an
 * uninitialized collection whose foreign key refers to a unique key of an entity,
 * rather than its identifier, look up the entity by that unique key.
 */
@SessionFactory
@DomainModel(annotatedClasses = { AuditPropertyRefLookupTest.Owner.class, AuditPropertyRefLookupTest.Item.class })
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditPropertyRefLookupTest$ChangesetIds"))
class AuditPropertyRefLookupTest {

	@Test
	void lookupByUniqueKeyAtChangeset(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// the identifier of each item is the code of the other
			final var first = new Item( 10, 20 );
			final var second = new Item( 20, 10 );
			session.persist( first );
			session.persist( second );
			final var owner = new Owner();
			owner.id = 1;
			owner.items.add( first );
			owner.labels.put( first, "first" );
			owner.labels.put( second, "second" );
			session.persist( owner );
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( 1 ).openSession()) {
			final var owner = session.find( Owner.class, 1 );
			final var first = session.find( Item.class, 10 );
			final var second = session.find( Item.class, 20 );
			assertTrue( Hibernate.contains( owner.items, first ) );
			assertFalse( Hibernate.contains( owner.items, second ) );
			assertFalse( Hibernate.isInitialized( owner.items ) );
			assertEquals( "first", Hibernate.get( owner.labels, first ) );
			assertEquals( "second", Hibernate.get( owner.labels, second ) );
			assertFalse( Hibernate.isInitialized( owner.labels ) );
		}
	}

	@Audited
	@Entity(name = "Owner")
	static class Owner {
		@Id
		int id;
		@ManyToMany
		@JoinTable(name = "Owner_items",
				inverseJoinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code"))
		Set<Item> items = new HashSet<>();
		@ElementCollection
		@CollectionTable(name = "Owner_labels")
		@MapKeyJoinColumn(name = "item_code", referencedColumnName = "code")
		Map<Item, String> labels = new HashMap<>();
	}

	@Audited
	@Entity(name = "Item")
	static class Item implements Serializable {
		@Id
		int id;
		@Column(unique = true)
		int code;

		Item() {
		}

		Item(int id, int code) {
			this.id = id;
			this.code = code;
		}
	}

	public static class ChangesetIds implements ChangesetIdentifierSupplier<Integer> {
		private int changeset;

		@Override
		public Integer generateIdentifier(SharedSessionContract session) {
			return ++changeset;
		}
	}
}
