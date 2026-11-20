package org.hibernate.orm.test.query.hql.size;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The subquery of a function or predicate of a collection is subject to
 * the restrictions and enabled filters of the collection.
 */
@DomainModel(annotatedClasses = {
		RestrictedCollectionSizeTest.Holder.class,
		RestrictedCollectionSizeTest.Item.class,
		RestrictedCollectionSizeTest.RestrictedItem.class
})
@SessionFactory
@JiraKey("HHH-21215")
public class RestrictedCollectionSizeTest {

	static List<String> collections() {
		return List.of(
				"restrictedOneToMany", "restrictedManyToMany", "restrictedJoinTable", "restrictedElements",
				"oneToManyRestricted", "manyToManyRestricted",
				"filteredOneToMany", "filteredManyToMany", "filteredJoinTable", "filteredElements"
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// the only element of the first holder is hidden, one of the two elements
			// of the second holder is hidden, and the third holder has no elements
			for ( int id = 1; id <= 3; id++ ) {
				session.persist( new Holder( id ) );
			}
			for ( int id = 1; id <= 3; id++ ) {
				final Holder holder = session.find( Holder.class, id == 1 ? 1 : 2 );
				final String kind = id == 3 ? "visible" : "hidden";
				for ( int n = 0; n < 6; n++ ) {
					final Item item = new Item( id * 10 + n, kind );
					session.persist( item );
					holder.items( n ).add( item );
				}
				for ( int n = 0; n < 2; n++ ) {
					final RestrictedItem restrictedItem = new RestrictedItem( id * 10 + n, kind );
					session.persist( restrictedItem );
					( n == 0 ? holder.oneToManyRestricted : holder.manyToManyRestricted ).add( restrictedItem );
				}
				holder.restrictedElements.add( kind + id );
				holder.filteredElements.add( kind + id );
			}
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@MethodSource("collections")
	public void sizeInWhereClause(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 2 ),
				ids( scope, "select h.id from Holder h where size(h." + collection + ") > 0 order by h.id" ) );
	}

	@ParameterizedTest
	@MethodSource("collections")
	public void sizeInSelectClause(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 0, 1, 0 ),
				ids( scope, "select size(h." + collection + ") from Holder h order by h.id" ) );
	}

	@ParameterizedTest
	@MethodSource("collections")
	public void maxElement(String collection, SessionFactoryScope scope) {
		assertEquals( List.of( 1, 3 ),
				ids( scope, "select h.id from Holder h where max(elements(h." + collection + ")) is null order by h.id" ) );
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 4})
	public void memberOf(int n, SessionFactoryScope scope) {
		// the restriction or the filter of the many-to-many association hides the element
		final String hql = "select h.id from Holder h where :item member of h."
				+ ( n == 1 ? "restrictedManyToMany" : "filteredManyToMany" );
		scope.inSession( session -> {
			session.enableFilter( "visible" );
			assertEquals( List.of(), session.createSelectionQuery( hql, Integer.class )
					.setParameter( "item", session.getReference( Item.class, 20 + n ) ).getResultList() );
			assertEquals( List.of( 2 ), session.createSelectionQuery( hql, Integer.class )
					.setParameter( "item", session.getReference( Item.class, 30 + n ) ).getResultList() );
		} );
	}

	private static List<Integer> ids(SessionFactoryScope scope, String hql) {
		return scope.fromSession( session -> {
			session.enableFilter( "visible" );
			return session.createSelectionQuery( hql, Integer.class ).getResultList();
		} );
	}

	@Entity(name = "Holder")
	@Table(name = "size_holder")
	@FilterDef(name = "visible", defaultCondition = "kind <> 'hidden'")
	public static class Holder {
		@Id
		Integer id;

		@OneToMany
		@JoinColumn(name = "restricted_holder_id")
		@SQLRestriction("kind <> 'hidden'")
		Set<Item> restrictedOneToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "size_holder_restricted")
		@SQLRestriction("kind <> 'hidden'")
		Set<Item> restrictedManyToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "size_holder_restricted_jt", inverseJoinColumns = @JoinColumn(name = "item_id"))
		@SQLJoinTableRestriction("item_id >= 30")
		Set<Item> restrictedJoinTable = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "size_holder_restricted_elements")
		@Column(name = "element_name")
		@SQLRestriction("element_name not like 'hidden%'")
		Set<String> restrictedElements = new HashSet<>();

		@OneToMany
		@JoinColumn(name = "filtered_holder_id")
		@Filter(name = "visible")
		Set<Item> filteredOneToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "size_holder_filtered")
		@Filter(name = "visible")
		Set<Item> filteredManyToMany = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "size_holder_filtered_jt", inverseJoinColumns = @JoinColumn(name = "item_id"))
		@FilterJoinTable(name = "visible", condition = "item_id >= 30")
		Set<Item> filteredJoinTable = new HashSet<>();

		@ElementCollection
		@CollectionTable(name = "size_holder_filtered_elements")
		@Column(name = "element_name")
		@Filter(name = "visible", condition = "element_name not like 'hidden%'")
		Set<String> filteredElements = new HashSet<>();

		// collections of an entity with a restriction of its own

		@OneToMany
		@JoinColumn(name = "holder_id")
		Set<RestrictedItem> oneToManyRestricted = new HashSet<>();

		@ManyToMany
		@JoinTable(name = "size_holder_restricted_items")
		Set<RestrictedItem> manyToManyRestricted = new HashSet<>();

		public Holder() {
		}

		public Holder(Integer id) {
			this.id = id;
		}

		Set<Item> items(int n) {
			return switch ( n ) {
				case 0 -> restrictedOneToMany;
				case 1 -> restrictedManyToMany;
				case 2 -> restrictedJoinTable;
				case 3 -> filteredOneToMany;
				case 4 -> filteredManyToMany;
				default -> filteredJoinTable;
			};
		}
	}

	@Entity(name = "Item")
	@Table(name = "size_item")
	public static class Item {
		@Id
		Integer id;
		String kind;

		public Item() {
		}

		public Item(Integer id, String kind) {
			this.id = id;
			this.kind = kind;
		}
	}

	@Entity(name = "RestrictedItem")
	@Table(name = "size_restricted_item")
	@SQLRestriction("kind <> 'hidden'")
	public static class RestrictedItem {
		@Id
		Integer id;
		String kind;

		public RestrictedItem() {
		}

		public RestrictedItem(Integer id, String kind) {
			this.id = id;
			this.kind = kind;
		}
	}
}
