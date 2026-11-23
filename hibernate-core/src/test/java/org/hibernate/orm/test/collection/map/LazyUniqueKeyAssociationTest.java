package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey("HHH-21229")
@BytecodeEnhanced
@DomainModel(annotatedClasses = {
		LazyUniqueKeyAssociationTest.Catalog.class,
		LazyUniqueKeyAssociationTest.Item.class,
		LazyUniqueKeyAssociationTest.RegionalItem.class
})
@SessionFactory
class LazyUniqueKeyAssociationTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			final var item = new Item();
			item.code = "KEY";
			session.persist( item );
		} );
	}

	@Test
	void insertMapKeyWithUnfetchedCode(SessionFactoryScope scope) {
		insertMapKey( scope, session -> session.find( Item.class, 1 ) );
	}

	@Test
	void insertMapKeyWithEnhancedProxy(SessionFactoryScope scope) {
		insertMapKey( scope, session -> session.getReference( Item.class, 1 ) );
	}

	private void insertMapKey(SessionFactoryScope scope, Function<Session, Item> loadItem) {
		scope.inTransaction( session -> {
			final var item = loadItem.apply( session );
			assertFalse( Hibernate.isPropertyInitialized( item, "code" ) );
			final var catalog = new Catalog();
			catalog.labels.put( item, "member" );
			session.persist( catalog );
		} );
		scope.inSession( session -> assertEquals( "KEY",
				session.createNativeQuery( "select key_code from catalog_labels", String.class ).getSingleResult() ) );
	}

	@Test
	void persistWithManyToOne(SessionFactoryScope scope) {
		insertWithManyToOne( scope, Session::persist );
	}

	@Test
	void mergeWithManyToOne(SessionFactoryScope scope) {
		insertWithManyToOne( scope, Session::merge );
	}

	private void insertWithManyToOne(SessionFactoryScope scope, BiConsumer<Session, Catalog> save) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, 1 );
			assertFalse( Hibernate.isPropertyInitialized( item, "code" ) );
			final var catalog = new Catalog();
			catalog.featuredItem = item;
			save.accept( session, catalog );
		} );
		scope.inSession( session -> assertEquals( "KEY",
				session.createNativeQuery( "select featured_code from Catalog", String.class ).getSingleResult() ) );
	}

	@Test
	void bindManyToOneParameter(SessionFactoryScope scope) {
		persistCatalogWithFeaturedItem( scope );
		scope.inSession( session -> {
			final var item = session.find( Item.class, 1 );
			assertFalse( Hibernate.isPropertyInitialized( item, "code" ) );
			assertEquals( 1, session.createSelectionQuery(
					"select id(c) from Catalog c where c.featuredItem = :item", Integer.class )
					.setParameter( "item", item )
					.getSingleResult() );
		} );
	}

	@Test
	void readKeySideFromEnhancedProxy(SessionFactoryScope scope) {
		persistCatalogWithFeaturedItem( scope );
		scope.inSession( session -> {
			// The key-side accessor follows Catalog.featuredItem to Item.code.
			Hibernate.initializeProperty( session.find( Item.class, 1 ), "code" );
			final var catalog = session.getReference( Catalog.class, 1 );
			assertFalse( Hibernate.isInitialized( catalog ) );
			final var association = (EntityAssociationMapping) session.getFactory().getMappingMetamodel()
					.getEntityDescriptor( Catalog.class ).findAttributeMapping( "featuredItem" );
			final var fk = association.getForeignKeyDescriptor();
			assertEquals( "KEY", fk.getAssociationKeyFromSide( catalog, fk.getKeySide(), session ) );
		} );
	}

	private void persistCatalogWithFeaturedItem(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var catalog = new Catalog();
			catalog.featuredItem = session.find( Item.class, 1 );
			Hibernate.initializeProperty( catalog.featuredItem, "code" );
			session.persist( catalog );
		} );
	}

	@Test
	void lookupCompositeMapKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = new RegionalItem();
			item.code = "KEY";
			session.persist( item );
			final var catalog = new Catalog();
			catalog.regionalLabels.put( item, "member" );
			session.persist( catalog );
		} );
		scope.inSession( session -> {
			final var labels = session.find( Catalog.class, 1 ).regionalLabels;
			final var item = session.getReference( RegionalItem.class, 1 );
			assertFalse( Hibernate.isInitialized( item ) );
			assertEquals( "member", Hibernate.get( labels, item ) );
			assertFalse( Hibernate.isInitialized( item ) );
			assertFalse( Hibernate.isInitialized( labels ) );
		} );
	}

	@Entity(name = "Catalog")
	static class Catalog {
		// This identifier and the referenced unique key on Item share a property name.
		@Id
		int code = 1;

		@ManyToOne
		@JoinColumn(name = "featured_code", referencedColumnName = "code")
		Item featuredItem;

		@ElementCollection
		@CollectionTable(name = "catalog_labels")
		@MapKeyJoinColumn(name = "key_code", referencedColumnName = "code")
		Map<Item, String> labels = new HashMap<>();

		@ElementCollection
		@MapKeyJoinColumns({
				@MapKeyJoinColumn(name = "key_code", referencedColumnName = "code"),
				@MapKeyJoinColumn(name = "key_region", referencedColumnName = "region")
		})
		Map<RegionalItem, String> regionalLabels = new HashMap<>();
	}

	@Entity(name = "Item")
	static class Item {
		@Id
		int id = 1;

		@Basic(fetch = LAZY)
		@Column(unique = true)
		String code;
	}

	@Entity(name = "RegionalItem")
	static class RegionalItem {
		@Id
		int id = 1;

		@Basic(fetch = LAZY)
		String code;

		String region = "EU";
	}
}
