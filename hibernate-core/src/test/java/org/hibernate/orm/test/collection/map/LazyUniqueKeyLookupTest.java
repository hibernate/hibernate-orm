package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyJoinColumn;

import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link Hibernate#get(Map, Object)} of an uninitialized map keyed by an entity
 * looks up the unique key the foreign key refers to without fetching it, when it
 * is a lazy attribute which has not been fetched, or the key is a detached proxy.
 */
@BytecodeEnhanced
@DomainModel(annotatedClasses = { LazyUniqueKeyLookupTest.Catalog.class, LazyUniqueKeyLookupTest.Item.class })
@SessionFactory
class LazyUniqueKeyLookupTest {

	@Test
	void getByKeyWithUnfetchedUniqueKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new Item();
			item.id = 1;
			item.code = "KEY";
			session.persist( item );
			final var catalog = new Catalog();
			catalog.labels.put( item, "label" );
			session.persist( catalog );
		} );
		scope.inSession( session -> {
			final var labels = session.find( Catalog.class, 1 ).labels;
			final var item = session.find( Item.class, 1 );
			assertFalse( Hibernate.isPropertyInitialized( item, "code" ) );
			assertEquals( "label", Hibernate.get( labels, item ) );
			assertFalse( Hibernate.isInitialized( labels ) );
			assertFalse( Hibernate.isPropertyInitialized( item, "code" ) );
		} );
	}

	@Test
	void getByDetachedEnhancedProxy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new Item();
			item.id = 2;
			item.code = "PROXY";
			session.persist( item );
			final var catalog = new Catalog();
			catalog.id = 2;
			catalog.labels.put( item, "proxied" );
			session.persist( catalog );
		} );
		final var detached = scope.fromSession( session -> session.getReference( Item.class, 2 ) );
		assertFalse( Hibernate.isInitialized( detached ) );
		scope.inSession( session -> {
			final var labels = session.find( Catalog.class, 2 ).labels;
			assertEquals( "proxied", Hibernate.get( labels, detached ) );
			session.find( Item.class, 2 );
			assertEquals( "proxied", Hibernate.get( labels, detached ) );
			assertFalse( Hibernate.isInitialized( labels ) );
			assertFalse( Hibernate.isInitialized( detached ) );
		} );
	}

	@Entity(name = "Catalog")
	static class Catalog {
		@Id
		int id = 1;
		@ElementCollection
		@CollectionTable(name = "Catalog_labels")
		@MapKeyJoinColumn(name = "item_code", referencedColumnName = "code")
		Map<Item, String> labels = new HashMap<>();
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		int id;
		@Basic(fetch = LAZY)
		@Column(unique = true)
		String code;
	}
}
