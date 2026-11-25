package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * An element of a collection whose join table references a unique key of the associated
 * entity is compared with an entity parameter by its identifier, and with an entity which
 * is compared by some other key by its identifier too.
 * <p>
 * Cart 1 holds items 1 and 2, cart 2 holds items 3 and 4, and cart 3 is empty. Item 5 is
 * in no cart. Every collection of a cart holds the same items, and the favorite item of
 * a cart is its last item.
 */
@DomainModel(annotatedClasses = {
		UniqueKeyJoinTableComparisonTest.Cart.class,
		UniqueKeyJoinTableComparisonTest.Item.class
})
@SessionFactory(useCollectingStatementObserver = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-21257")
class UniqueKeyJoinTableComparisonTest {

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"c.favorite <> :item | 1 | 2 | A,A,-",
			"c.favorite is null or c.favorite = :item | 2 | 2 | B,D,B"
	})
	void updateFavoriteWithParameterAlsoUsedInPredicate(
			String predicate, long itemId, int updated, String expectedCodes, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var item = session.find( Item.class, itemId );
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();
			assertEquals( updated, session.createMutationQuery( "update Cart c set c.favorite = :item where " + predicate )
					.setParameter( "item", item ).executeUpdate() );
			assertEquals( 1, observer.getSqlQueries().size() );
			assertEquals( List.of( expectedCodes.split( "," ) ), session.createNativeQuery(
					"select coalesce(favorite_code, '-') from Cart order by id", String.class )
					.getResultList() );
		} );
	}

	@Test
	void deleteCartByFavorite(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1, session.createMutationQuery( "delete from Cart c where c.favorite = :item" )
					.setParameter( "item", session.getReference( Item.class, 2L ) ).executeUpdate() );
			assertEquals( List.of( 2L, 3L ), session.createQuery( "select c.id from Cart c order by c.id", Long.class )
					.getResultList() );
			assertEquals( List.of( "C", "D" ), session.createNativeQuery(
					"select item_code from cart_items order by item_code", String.class ).getResultList() );
		} );
	}

	@Test
	void selectingFavoriteCodePreservesNullFavorite(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var rows = session.createQuery(
					"select c.id, c.favorite.code from Cart c where c.id = 3 or c.favorite = :item order by c.id",
					Object[].class ).setParameter( "item", session.getReference( Item.class, 2L ) ).getResultList();
			assertEquals( 2, rows.size() );
			assertArrayEquals( new Object[] { 1L, "B" }, rows.get( 0 ) );
			assertArrayEquals( new Object[] { 3L, null }, rows.get( 1 ) );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsSubqueryInOnClause.class)
	void correlatedFavoriteInJoinPredicate(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of( 1L, 1L, 2L, 3L ), session.createQuery(
				"select c.id from Cart c left join c.items i on exists "
						+ "(select 1 from Item r where c.favorite = :item) order by c.id", Long.class )
				.setParameter( "item", session.getReference( Item.class, 2L ) ).getResultList() ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void toOneComparedWithEntityLiteral(boolean managed, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item item;
			if ( managed ) {
				item = session.find( Item.class, 2L );
			}
			else {
				// a literal is compared by the key it holds, so it needs no identifier
				item = new Item();
				item.code = "B";
			}
			final var builder = session.getCriteriaBuilder();
			final var equal = builder.createQuery( Long.class );
			final var cart = equal.from( Cart.class );
			equal.select( cart.get( "id" ) ).where( builder.equal( cart.get( "favorite" ), builder.literal( item ) ) );
			assertEquals( List.of( 1L ), session.createQuery( equal ).getResultList() );
			final var in = builder.createQuery( Long.class );
			final var other = in.from( Cart.class );
			in.select( other.get( "id" ) ).where( other.get( "favorite" ).in( builder.literal( item ) ) );
			assertEquals( List.of( 1L ), session.createQuery( in ).getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "item in (cart.favorite, :value)", "item in (:value, cart.favorite)" })
	void toOneAndParameterUseIdentifier(String predicate, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Item reference = session.getReference( Item.class, 1L );
			observer.clear();
			assertEquals( List.of( 1L, 2L ), session.createQuery(
					"select item.id from Cart cart join cart.items item, Item root "
							+ "where cart.id = 1 and " + predicate + " and root = :value order by item.id", Long.class )
					.setParameter( "value", reference ).getResultList() );
			assertFalse( Hibernate.isInitialized( reference ) );
			assertEquals( 1, observer.getSqlQueries().size() );
		} );
	}

	@Test
	void toOneAndDetachedParameterUseIdentifier(SessionFactoryScope scope) {
		final Item reference = scope.fromTransaction( session -> session.getReference( Item.class, 1L ) );
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			observer.clear();
			assertEquals( List.of( 1L, 2L ), session.createQuery(
					"select item.id from Cart cart join cart.items item where cart.id = 1 "
							+ "and item in (cart.favorite, :value) order by item.id", Long.class )
					.setParameter( "value", reference ).getResultList() );
			assertEquals( 1, observer.getSqlQueries().size() );
		} );
		assertFalse( Hibernate.isInitialized( reference ) );
	}

	@Test
	void nullParameterWithToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of( 2L ), session.createQuery(
				"select item.id from Cart cart join cart.items item where cart.id = 1 "
						+ "and item in (cart.favorite, :value) order by item.id", Long.class )
				.setParameter( "value", null ).getResultList() ) );
	}

	@Test
	void parameterOnLeftOfInWithToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item reference = session.getReference( Item.class, 1L );
			assertEquals( List.of( 1L ), session.createQuery(
					"select item.id from Cart cart join cart.items item where cart.id = 1 "
							+ "and :value in (cart.favorite, item) order by item.id", Long.class )
					.setParameter( "value", reference ).getResultList() );
			assertFalse( Hibernate.isInitialized( reference ) );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "cart.favorite = :value", ":value = cart.favorite" })
	void toOneComparisonWithParameter(String predicate, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Item reference = session.getReference( Item.class, 2L );
			observer.clear();
			assertEquals( List.of( 1L ), session.createQuery(
					"select cart.id from Cart cart where " + predicate, Long.class )
					.setParameter( "value", reference ).getResultList() );
			assertFalse( Hibernate.isInitialized( reference ) );
			assertEquals( 1, observer.getSqlQueries().size() );
		} );
	}

	@Test
	void nullToOneMakesNotInUnknown(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of(), session.createQuery(
				"select cart.id from Cart cart where cart.id = 3 and :value not in (cart.favorite)", Long.class )
				.setParameter( "value", session.getReference( Item.class, 1L ) ).getResultList() ) );
	}

	@Test
	void nullableToOneInCorrelatedSubquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of( 3L ), session.createQuery(
				"select cart.id from Cart cart where cart.id = 3 and exists "
						+ "(select 1 from Item root where cart.id = 3 or cart.favorite = :value)", Long.class )
				.setParameter( "value", session.getReference( Item.class, 1L ) ).getResultList() ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "cart.favorite in (:value, cart.favorite)",
			"cart.favorite not in (:value, cart.favorite)",
			"cart.favorite between :value and cart.favorite" })
	void nullableToOneRetainsOwnerInDisjunction(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item reference = session.getReference( Item.class, 1L );
			assertEquals( List.of( 3L ), session.createQuery(
					"select cart.id from Cart cart where cart.id = 3 and (cart.id = 3 or "
							+ predicate + ")", Long.class )
					.setParameter( "value", reference ).getResultList() );
			assertFalse( Hibernate.isInitialized( reference ) );
		} );
	}

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item[] items = new Item[6];
			for ( int i = 1; i <= 5; i++ ) {
				items[i] = new Item();
				items[i].id = i;
				items[i].code = String.valueOf( (char) ( 'A' + i - 1 ) );
				items[i].alternateCode = String.valueOf( (char) ( 'V' + i - 1 ) );
				session.persist( items[i] );
			}
			for ( int id = 1; id <= 3; id++ ) {
				final Cart cart = new Cart();
				cart.id = id;
				if ( id < 3 ) {
					for ( Item item : List.of( items[2 * id - 1], items[2 * id] ) ) {
						cart.items.add( item );
						cart.alternateItems.add( item );
						cart.pkItems.add( item );
					}
					cart.favorite = items[2 * id];
				}
				session.persist( cart );
			}
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(strings = { "item = other", "other = item", "item in (other)" })
	void elementsWithDifferentKeys(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of( 1L, 2L ), session.createQuery(
				"select item.id from Cart cart join cart.items item join cart.alternateItems other "
						+ "where cart.id = 1 and " + predicate + " order by item.id",
				Long.class ).getResultList() ) );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"item in (:value, other) | 1,1,2",
			"item in (other, :value) | 1,1,2",
			"item not in (:value, other) | 2",
			"item between other and :value | 1"
	})
	void elementsWithDifferentKeysAndParameter(String predicate, String expected, SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( expected, ids( session.createQuery(
						"select item.id from Cart cart join cart.items item join cart.alternateItems other "
								+ "where cart.id = 1 and " + predicate + " order by item.id",
						Long.class )
				.setParameter( "value", session.find( Item.class, 1L ) )
				.getResultList() ) ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"item in (cart.favorite, :value)", "item in (:value, cart.favorite)",
			"item between :value and cart.favorite", "cart.favorite in (:value, item)",
			"item in (cart.favorite, other, :value)"
	})
	void elementComparedWithToOneAndParameter(String predicate, SessionFactoryScope scope) {
		final String from = predicate.contains( "other" )
				? "Cart cart join cart.items item join cart.alternateItems other"
				: "Cart cart join cart.items item";
		scope.inTransaction( session -> assertEquals( predicate.startsWith( "cart.favorite" ) ? "2" : predicate.contains( "other" ) ? "1,1,2,2" : "1,2",
				ids( session.createQuery(
								"select item.id from " + from + " where cart.id = 1 and " + predicate + " order by item.id",
								Long.class )
						.setParameter( "value", session.find( Item.class, 1L ) )
						.getResultList() ) ) );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"item = :value and root = :value", "root = :value and item = :value",
			"item = root and item = :value", "item in (root, :value) and root = :value"
	})
	void parameterComparedWithElementAndWithRoot(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( 1L, session.createQuery(
						"select count(*) from Cart cart join cart.items item, Item root where " + predicate,
						Long.class )
				.setParameter( "value", session.find( Item.class, 1L ) )
				.getSingleResult() ) );
	}

	@ParameterizedTest
	@CsvSource({
			"items,item = :value", "items,:value = item", "items,item in (:value)", "items,item between :value and :value",
			"pkItems,item = :value", "pkItems,:value = item", "pkItems,item in (:value)", "pkItems,item between :value and :value"
	})
	void parameterIsNotInitialized(String collection, String predicate, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Item reference = session.getReference( Item.class, 1L );
			observer.clear();
			assertEquals( 1L, session.createQuery(
							"select count(*) from Cart cart join cart." + collection + " item where " + predicate,
							Long.class )
					.setParameter( "value", reference )
					.getSingleResult() );
			assertFalse( Hibernate.isInitialized( reference ) );
			assertEquals( 1, observer.getSqlQueries().size() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "items", "pkItems" })
	void multiValuedParameterIsNotInitialized(String collection, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Item first = session.getReference( Item.class, 1L );
			final Item second = session.getReference( Item.class, 3L );
			observer.clear();
			assertEquals( 2L, session.createQuery(
							"select count(*) from Cart cart join cart." + collection + " item where item in :values",
							Long.class )
					.setParameter( "values", List.of( first, second ) )
					.getSingleResult() );
			assertFalse( Hibernate.isInitialized( first ) );
			assertFalse( Hibernate.isInitialized( second ) );
			assertEquals( 1, observer.getSqlQueries().size() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			":value member of cart.items",
			":value in elements(cart.items)",
			":value in (select e from cart.items e)",
			"element(cart.items) = :value",
			"exists (select 1 from cart.items e where e = :value)"
	})
	void parameterComparedWithElements(String predicate, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			final Item reference = session.getReference( Item.class, 3L );
			observer.clear();
			assertEquals( List.of( 2L ), session.createQuery(
							"select cart.id from Cart cart where " + predicate,
							Long.class )
					.setParameter( "value", reference )
					.getResultList() );
			assertFalse( Hibernate.isInitialized( reference ) );
			assertEquals( 1, observer.getSqlQueries().size() );
		} );
	}

	@Test
	void parameterNotMemberOf(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( List.of( 1L, 3L ), session.createQuery(
						"select cart.id from Cart cart where :value not member of cart.items order by cart.id",
						Long.class )
				.setParameter( "value", session.find( Item.class, 3L ) )
				.getResultList() ) );
	}

	@Test
	void detachedParameterIsNotInitialized(SessionFactoryScope scope) {
		final Item detached = scope.fromTransaction( session -> session.getReference( Item.class, 1L ) );
		scope.inTransaction( session -> assertEquals( 1L, session.createQuery(
						"select count(*) from Cart cart join cart.items item where item = :value",
						Long.class )
				.setParameter( "value", detached )
				.getSingleResult() ) );
		assertFalse( Hibernate.isInitialized( detached ) );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"item = (select i from Item i where i.id = 1) | 1",
			"(select i from Item i where i.id = 1) = item | 1",
			"item in (select i from Item i) | 1,2",
			"item in (select e from cart.items e) | 1,2",
			"item in (select e from cart.alternateItems e) | 1,2",
			"item in (select e from cart.pkItems e) | 1,2"
	})
	void elementComparedWithSubquery(String predicate, String expected, SessionFactoryScope scope) {
		scope.inTransaction( session -> assertEquals( expected, ids( session.createQuery(
				"select item.id from Cart cart join cart.items item where cart.id = 1 and " + predicate
						+ " order by item.id",
				Long.class ).getResultList() ) ) );
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"item = any (select i from Item i) | 1,2",
			"item <> all (select i from Item i where i.id > 1) | 1"
	})
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsAllQuantifiedSubqueriesWithComparison.class)
	void elementComparedWithQuantifiedSubquery(String predicate, String expected, SessionFactoryScope scope) {
		elementComparedWithSubquery( predicate, expected, scope );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"item = root", "root = item", "item in (root)", "root in (item)", "item between root and root",
			"element(cart.items) = root", "root member of cart.items"
	})
	void elementComparedWithRootByKeyOfJoinTable(String predicate, SessionFactoryScope scope) {
		final var observer = scope.getCollectingStatementObserver();
		final String from = predicate.contains( "cart.items" ) ? "Cart cart" : "Cart cart join cart.items item";
		scope.inTransaction( session -> {
			observer.clear();
			assertEquals( List.of( 1L, 2L ), session.createQuery(
					"select root.id from " + from + ", Item root where cart.id = 1 and " + predicate + " order by root.id",
					Long.class ).getResultList() );
			// the entity is compared by the key which the join table holds, so its table is joined only once, as the root
			assertEquals( 1, occurrences( observer.getSqlQueries().get( 0 ), "Item " ) );
		} );
	}

	private static String ids(List<Long> ids) {
		final StringBuilder list = new StringBuilder();
		for ( Long id : ids ) {
			if ( !list.isEmpty() ) {
				list.append( ',' );
			}
			list.append( id );
		}
		return list.toString();
	}

	private static int occurrences(String sql, String text) {
		int count = 0;
		for ( int index = sql.indexOf( text ); index >= 0; index = sql.indexOf( text, index + 1 ) ) {
			count++;
		}
		return count;
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		public long id;

		@Column(unique = true)
		public String code;

		@Column(unique = true)
		public String alternateCode;
	}

	@Entity(name = "Cart")
	public static class Cart {
		@Id
		public long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "favorite_code", referencedColumnName = "code")
		public Item favorite;

		@ManyToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code")
		)
		public Set<Item> items = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_alternate_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_alternate_code", referencedColumnName = "alternateCode")
		)
		public Set<Item> alternateItems = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_pk_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_id")
		)
		public Set<Item> pkItems = new HashSet<>();
	}
}
