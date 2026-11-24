package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.criteria.JoinType;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A right join of a many-to-many association returns every entity of the associated
 * type which is visible, with each owner whose collection it belongs to, or with no
 * owner at all.
 * <p>
 * Cart 1 holds items 1 and 2, cart 2 holds items 1 and 3, and cart 3 is empty. Items
 * 4 and 5 are in no cart. Items 3 and 5 are out of stock.
 */
@DomainModel(annotatedClasses = {
		ManyToManyRightJoinTest.Cart.class,
		ManyToManyRightJoinTest.Item.class,
		ManyToManyRightJoinTest.Coupon.class,
		ManyToManyRightJoinTest.Marker.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21258")
class ManyToManyRightJoinTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item[] items = new Item[6];
			for ( int i = 1; i <= 5; i++ ) {
				items[i] = new Item();
				items[i].id = i;
				items[i].code = String.valueOf( (char) ( 'A' + i - 1 ) );
				items[i].stock = i == 3 || i == 5 ? 0 : 10;
				session.persist( items[i] );
			}
			final Coupon[] coupons = new Coupon[4];
			for ( int i = 1; i <= 3; i++ ) {
				coupons[i] = new Coupon();
				coupons[i].id = i;
				session.persist( coupons[i] );
			}
			final Cart cart1 = new Cart();
			cart1.id = 1;
			cart1.items.add( items[1] );
			cart1.items.add( items[2] );
			cart1.rankedItems.add( items[2] );
			cart1.rankedItems.add( items[1] );
			cart1.coupons.add( coupons[1] );
			cart1.coupons.add( coupons[2] );
			final Cart cart2 = new Cart();
			cart2.id = 2;
			cart2.items.add( items[1] );
			cart2.items.add( items[3] );
			final Cart cart3 = new Cart();
			cart3.id = 3;
			session.persist( cart1 );
			session.persist( cart2 );
			session.persist( cart3 );
			for ( int id : List.of( 1, 3, 4, 9 ) ) {
				final Marker marker = new Marker();
				marker.id = id;
				session.persist( marker );
			}
		} );
		// coupon 2 belongs to cart 1, and coupon 3 to no cart
		scope.inTransaction( session -> session.remove( session.find( Coupon.class, 2L ) ) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void rightJoin(SessionFactoryScope scope) {
		assertEquals( List.of( "1:1", "2:1", "1:2", "2:3", "null:4", "null:5" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.items item order by item.id, cart.id" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "count(*)", "count(item)", "count(item.code)" })
	void rightJoinCount(String selection, SessionFactoryScope scope) {
		assertEquals( List.of( "6" ),
				rows( scope, "select " + selection + " from Cart cart right join cart.items item" ) );
	}

	@Test
	void rightJoinOfEntityInNoCollection(SessionFactoryScope scope) {
		assertEquals( List.of( "4", "5" ),
				rows( scope, "select item.id from Cart cart right join cart.items item where cart.id is null order by item.id" ) );
		scope.inTransaction( session -> assertEquals( List.of( "D", "E" ),
				session.createQuery( "select item from Cart cart right join cart.items item where cart is null order by item.id", Item.class )
						.getResultList().stream().map( item -> item.code ).toList() ) );
	}

	@Test
	void rightJoinRestrictedByKeyOfEntity(SessionFactoryScope scope) {
		assertEquals( List.of( "null:4" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.items item where item.id = 4" ) );
		assertEquals( List.of( "1:1", "2:1", "null:2", "null:3", "null:4", "null:5" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.items item on item.id = 1 order by item.id, cart.id" ) );
	}

	@Test
	void rightJoinWithJoinTableRestriction(SessionFactoryScope scope) {
		// the restriction hides the items of cart 2, so item 1 belongs to cart 1 alone, and item 3 to no cart
		assertEquals( List.of( "1:1", "1:2", "null:3", "null:4", "null:5" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.itemsOutsideCartTwo item order by item.id, cart.id" ) );
		assertEquals( List.of( "5" ),
				rows( scope, "select count(*) from Cart cart right join cart.itemsOutsideCartTwo item" ) );
	}

	@Test
	void rightJoinWithJoinTableFilter(SessionFactoryScope scope) {
		final String hql = "select cart.id, item.id from Cart cart right join cart.filteredItems item order by item.id, cart.id";
		assertEquals( List.of( "1:1", "2:1", "1:2", "2:3", "null:4", "null:5" ), rows( scope, hql ) );
		assertEquals( List.of( "1:1", "1:2", "null:3", "null:4", "null:5" ), rows( scope, true, hql ) );
	}

	@Test
	void rightJoinWithPredicateOnOwner(SessionFactoryScope scope) {
		assertEquals( List.of( "1:1", "1:2", "null:3", "null:4", "null:5" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.items item on cart.id <> 2 order by item.id, cart.id" ) );
	}

	@Test
	void rightJoinWithPredicateOnElement(SessionFactoryScope scope) {
		assertEquals( List.of( "1:1", "2:1", "null:2", "null:3", "null:4", "null:5" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.items item on item.code = 'A' order by item.id, cart.id" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void rightJoinWithRestrictionOnElement(SessionFactoryScope scope) {
		// an item which is out of stock is not an element of the collection, so it is never returned
		assertEquals( List.of( "1:1", "2:1", "1:2", "null:4" ),
				rows( scope, "select cart.id, item.id from Cart cart right join cart.availableItems item order by item.id, cart.id" ) );
		assertEquals( List.of( "4" ),
				rows( scope, "select count(*) from Cart cart right join cart.availableItems item" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void rightJoinOfSoftDeletedEntity(SessionFactoryScope scope) {
		assertEquals( List.of( "1:1", "null:3" ),
				rows( scope, "select cart.id, coupon.id from Cart cart right join cart.coupons coupon order by coupon.id" ) );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void laterRightJoinKeepsItsRows(SessionFactoryScope scope) {
		// every marker survives the last join, with no item for item 3, which is out of stock, and for the missing item 9
		assertEquals( List.of( "1:1:1", "1:1:2", "3:null:null", "4:4:null", "9:null:null" ),
				rows( scope, "select marker.id, item.id, cart.id from Cart cart right join cart.availableItems item "
						+ "right join Marker marker on marker.id = item.id order by marker.id, cart.id" ) );
	}

	@Test
	void rightJoinAfterJoinOfOwner(SessionFactoryScope scope) {
		// cart 2 has no marker, so item 1 belongs to cart 1 alone, and item 3 to no cart
		assertEquals( List.of( "1:1:1", "1:1:2", "null:null:3", "null:null:4", "null:null:5" ),
				rows( scope, "select marker.id, cart.id, item.id from Marker marker join Cart cart on cart.id = marker.id "
						+ "right join cart.items item order by item.id" ) );
	}

	@Test
	void rightJoinOfList(SessionFactoryScope scope) {
		assertEquals( List.of( "1:1:1", "1:2:0", "null:3:null", "null:4:null", "null:5:null" ),
				rows( scope, "select cart.id, item.id, index(item) from Cart cart right join cart.rankedItems item order by item.id" ) );
	}

	@Test
	void criteriaRightJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var query = builder.createQuery( Long.class );
			final var cart = query.from( Cart.class );
			final var item = cart.<Item>join( "items", JoinType.RIGHT );
			query.select( item.get( "id" ) ).where( cart.get( "id" ).isNull() ).orderBy( builder.asc( item.get( "id" ) ) );
			assertEquals( List.of( 4L, 5L ), session.createQuery( query ).getResultList() );
		} );
	}

	private List<String> rows(SessionFactoryScope scope, String hql) {
		return rows( scope, false, hql );
	}

	private List<String> rows(SessionFactoryScope scope, boolean enableFilter, String hql) {
		return scope.fromTransaction( session -> {
			if ( enableFilter ) {
				session.enableFilter( "notCartTwo" );
			}
			final List<String> result = new ArrayList<>();
			for ( Object row : session.createQuery( hql, Object.class ).getResultList() ) {
				if ( row instanceof Object[] values ) {
					final StringBuilder text = new StringBuilder();
					for ( Object value : values ) {
						if ( !text.isEmpty() ) {
							text.append( ':' );
						}
						text.append( value );
					}
					result.add( text.toString() );
				}
				else {
					result.add( String.valueOf( row ) );
				}
			}
			return result;
		} );
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		public long id;

		@Column(unique = true)
		public String code;

		public int stock;
	}

	@Entity(name = "Coupon")
	@SoftDelete
	public static class Coupon {
		@Id
		public long id;
	}

	@Entity(name = "Marker")
	public static class Marker {
		@Id
		public long id;
	}

	@Entity(name = "Cart")
	@FilterDef(name = "notCartTwo")
	public static class Cart {
		@Id
		public long id;

		@ManyToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_id")
		)
		public Set<Item> items = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(name = "item_id", insertable = false, updatable = false)
		)
		@SQLJoinTableRestriction("cart_id <> 2")
		public Set<Item> itemsOutsideCartTwo = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(name = "item_id", insertable = false, updatable = false)
		)
		@FilterJoinTable(name = "notCartTwo", condition = "cart_id <> 2")
		public Set<Item> filteredItems = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(name = "item_id", insertable = false, updatable = false)
		)
		@SQLRestriction("stock > 0")
		public Set<Item> availableItems = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_ranked_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_id")
		)
		@OrderColumn(name = "item_position")
		public List<Item> rankedItems = new ArrayList<>();

		@ManyToMany
		@JoinTable(
				name = "cart_coupons",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "coupon_id")
		)
		public Set<Coupon> coupons = new HashSet<>();
	}
}
