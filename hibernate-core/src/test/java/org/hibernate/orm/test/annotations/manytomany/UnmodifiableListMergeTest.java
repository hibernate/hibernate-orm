package org.hibernate.orm.test.annotations.manytomany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.annotations.Bag;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Merging a many-to-many bag must not modify an unmodifiable backing list.
///
/// @author Steve Ebersole
@JiraKey("HHH-19007")
@JiraKey("HHH-1914")
@DomainModel(annotatedClasses = { UnmodifiableListMergeTest.Owner.class, UnmodifiableListMergeTest.Item.class })
@SessionFactory
public class UnmodifiableListMergeTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1L ) );
			session.persist( new Item( 2L ) );
		} );
	}

	@AfterEach
	void cleanUp(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@EnumSource(ListKind.class)
	void testMergeTransientOwner(ListKind kind, SessionFactoryScope scope) {
		final var items = kind.create( new Item( 1L ) );
		final var original = new Owner( 1L, items );
		scope.inTransaction( session -> {
			final var merged = session.merge( original );
			assertNotSame( original, merged );
			assertSame( items, original.items );
			assertEquals( List.of( 1L ), itemIds( original ) );
			assertEquals( List.of( 1L ), itemIds( merged ) );
		} );
		assertStoredItems( scope, 1L );
	}

	@ParameterizedTest
	@EnumSource(ListKind.class)
	void testMergeDetachedOwner(ListKind kind, SessionFactoryScope scope) {
		final var original = new Owner( 1L, new ArrayList<>() );
		scope.inTransaction( session -> {
			original.items.add( session.find( Item.class, 1L ) );
			session.persist( original );
		} );
		final var items = kind.create( new Item( 2L ) );
		original.items = items;
		scope.inTransaction( session -> {
			final var merged = session.merge( original );
			assertNotSame( original, merged );
			assertSame( items, original.items );
			assertEquals( List.of( 2L ), itemIds( original ) );
			assertEquals( List.of( 2L ), itemIds( merged ) );
		} );
		assertStoredItems( scope, 2L );
	}

	@ParameterizedTest
	@EnumSource(ListKind.class)
	void testMergeManagedOwnerAndModifyResult(ListKind kind, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var original = new Owner( 1L, kind.create( session.find( Item.class, 1L ) ) );
			session.persist( original );
			session.flush();

			final var merged = session.merge( original );
			assertSame( original, merged );
			assertEquals( List.of( 1L ), itemIds( merged ) );
			merged.items.clear();
			merged.items.add( session.find( Item.class, 2L ) );
		} );
		assertStoredItems( scope, 2L );
	}

	private void assertStoredItems(SessionFactoryScope scope, Long expectedId) {
		scope.inTransaction( session ->
				assertEquals( List.of( expectedId ), itemIds( session.find( Owner.class, 1L ) ) ) );
	}

	private static List<Long> itemIds(Owner owner) {
		return owner.items.stream().map( item -> item.id ).toList();
	}

	enum ListKind {
		IMMUTABLE,
		UNMODIFIABLE,
		FIXED_SIZE;

		List<Item> create(Item item) {
			return switch ( this ) {
				case IMMUTABLE -> List.of( item );
				case UNMODIFIABLE -> Collections.unmodifiableList( new ArrayList<>( List.of( item ) ) );
				case FIXED_SIZE -> Arrays.asList( item );
			};
		}
	}

	@Entity(name = "UnmodifiableListOwner")
	public static class Owner {
		@Id
		private Long id;

		@ManyToMany
		@Bag
		private List<Item> items;

		public Owner() {
		}

		Owner(Long id, List<Item> items) {
			this.id = id;
			this.items = items;
		}
	}

	@Entity(name = "UnmodifiableListItem")
	public static class Item {
		@Id
		private Long id;

		public Item() {
		}

		Item(Long id) {
			this.id = id;
		}
	}
}
