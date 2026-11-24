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

import static org.hibernate.audit.AuditLog.ALL_CHANGESETS;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * At a changeset, {@link Hibernate#contains} and {@link Hibernate#get} of an
 * uninitialized collection whose foreign key refers to a unique key of an entity,
 * rather than its identifier, look up the entity by that unique key.
 */
@SessionFactory(useCollectingStatementObserver = true)
@DomainModel(annotatedClasses = { AuditPropertyRefLookupTest.Owner.class, AuditPropertyRefLookupTest.Item.class })
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditPropertyRefLookupTest$ChangesetIds"))
class AuditPropertyRefLookupTest {

	@Test
	void lookupByUniqueKeyAtChangeset(SessionFactoryScope scope) {
		final var changeset = scope.fromTransaction( session -> {
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
			return session.getCurrentChangesetIdentifier();
		} );
		try (var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).openSession()) {
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

	@Test
	void subselectFetchKeepsCollectionRevisionsSeparate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new Item( 30, 40 );
			item.tags.add( "first" );
			session.persist( item );
			session.persist( new Item( 40, 30 ) );
		} );
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			session.find( Item.class, 40 );
			final var item = session.find( Item.class, 30 );
			item.tags.add( "second" );
			observer.clear();
		} );
		// One collection row, its audit row, and the owner revision.
		observer.assertQueries().hasSize( 3 ).allMatch( sql -> sql.startsWith( "insert" ) );
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( ALL_CHANGESETS ).openSession() ) {
			session.setSubselectFetchingEnabled( true );
			final var revisions = session.createSelectionQuery(
					"from Item i where i.id = 30 order by changesetId(i)", Item.class ).getResultList();
			assertEquals( 2, revisions.size() );
			assertEquals( Set.of( "first" ), revisions.get( 0 ).tags );
			assertEquals( Set.of( "first", "second" ), revisions.get( 1 ).tags );
			assertEquals( 1L, session.createSelectionQuery(
					"select count(i) from Item i where i.id = 40", Long.class ).getSingleResult() );
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
		@ElementCollection
		@CollectionTable(name = "Item_tags", joinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code"))
		Set<String> tags = new HashSet<>();

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
