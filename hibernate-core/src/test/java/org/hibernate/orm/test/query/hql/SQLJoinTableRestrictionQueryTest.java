package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = {
		SQLJoinTableRestrictionQueryTest.Cart.class,
		SQLJoinTableRestrictionQueryTest.Item.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18996")
class SQLJoinTableRestrictionQueryTest {
	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Cart cart = new Cart();
			cart.id = 1;
			final Cart cartWithOnlyRestrictedItems = new Cart();
			cartWithOnlyRestrictedItems.id = 2;
			final Cart emptyCart = new Cart();
			emptyCart.id = 3;

			final Item visibleItem = new Item();
			visibleItem.id = 1;
			visibleItem.code = "A";
			final Item restrictedItem = new Item();
			restrictedItem.id = 2;
			restrictedItem.code = "B";
			final Item anotherRestrictedItem = new Item();
			anotherRestrictedItem.id = 3;
			anotherRestrictedItem.code = "C";
			final Item additionalRestrictedItem = new Item();
			additionalRestrictedItem.id = 4;
			additionalRestrictedItem.code = "D";
			final Item unassociatedItem = new Item();
			unassociatedItem.id = 5;
			unassociatedItem.code = "E";

			cart.unrestrictedItems.add( visibleItem );
			cart.unrestrictedItems.add( restrictedItem );
			cart.unrestrictedPkItems.add( visibleItem );
			cart.unrestrictedPkItems.add( restrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedItems.add( anotherRestrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedItems.add( additionalRestrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedPkItems.add( anotherRestrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedPkItems.add( additionalRestrictedItem );

			session.persist( cart );
			session.persist( cartWithOnlyRestrictedItems );
			session.persist( emptyCart );

			session.persist( visibleItem );
			session.persist( restrictedItem );
			session.persist( anotherRestrictedItem );
			session.persist( additionalRestrictedItem );
			session.persist( unassociatedItem );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@ValueSource(strings = {"items", "pkItems", "manyToManyItems", "manyToManyPkItems"})
	void innerJoinAppliesRestriction(String collection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String fromClause = " from Cart cart join cart." + collection + " item where cart.id = :id";
			assertEquals( 1L, session.createQuery(
					"select count(item)" + fromClause,
					Long.class
			).setParameter( "id", 1 ).getSingleResult() );
			assertEquals( 1L, session.createQuery(
					"select count(*)" + fromClause,
					Long.class
			).setParameter( "id", 1 ).getSingleResult() );
			assertEquals( 1L, session.createQuery(
					"select count(cart)" + fromClause,
					Long.class
			).setParameter( "id", 1 ).getSingleResult() );
			assertEquals( List.of( 1L ), session.createQuery(
					"select cart.id" + fromClause,
					Long.class
			).setParameter( "id", 1 ).getResultList() );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"items,2", "pkItems,2", "manyToManyItems,2", "manyToManyPkItems,2",
			"items,3", "pkItems,3", "manyToManyItems,3", "manyToManyPkItems,3"
	})
	void leftJoinPreservesCartWithoutVisibleItems(String collection, long cartId, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String fromClause = " from Cart cart left join cart." + collection + " item where cart.id = :id";
			assertEquals( 0L, session.createQuery(
					"select count(item)" + fromClause,
					Long.class
			).setParameter( "id", cartId ).getSingleResult() );
			assertEquals( 1L, session.createQuery(
					"select count(*)" + fromClause,
					Long.class
			).setParameter( "id", cartId ).getSingleResult() );
			assertEquals( 1L, session.createQuery(
					"select count(cart)" + fromClause,
					Long.class
			).setParameter( "id", cartId ).getSingleResult() );
			assertEquals( List.of( cartId ), session.createQuery(
					"select cart.id" + fromClause,
					Long.class
			).setParameter( "id", cartId ).getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"element(cart.items) = i", "i = element(cart.items)"})
	void entityPathComparisonsApplyRestriction(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String query = "select count(cart) from Cart cart, Item i where " + predicate;
			assertEquals( 1L, session.createQuery(
					query + " and i.id = 1",
					Long.class
			).getSingleResult() );
			assertEquals( 0L, session.createQuery(
					query + " and i.id = 2",
					Long.class
			).getSingleResult() );
			assertEquals( 0L, session.createQuery(
					query + " and i.id = 3",
					Long.class
			).getSingleResult() );
		} );
	}

	@Test
	void entityPathInequalityAppliesRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1L, session.createQuery(
					"select count(cart) from Cart cart, Item i "
					+ "where element(cart.items) <> i and i.id = 2",
					Long.class
			).getSingleResult() );
		} );
	}

	@Test
	void entityPathComparisonAppliesRestrictionToCartIdSelection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( List.of( 1L ), session.createQuery(
					"select cart.id from Cart cart, Item i "
					+ "where element(cart.items) = i and i.code = 'A'",
					Long.class
			).getResultList() );
		} );
	}

	@Test
	void explicitJoinEntityComparisonAppliesRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1L, session.createQuery(
					"select count(cart) from Cart cart join cart.items item, Item i "
					+ "where item = i and i.id = 1",
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = :item", ":item = item"})
	void entityParameterComparisonsApplyRestriction(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item restrictedItem = session.find( Item.class, 2L );
			final String query = "select count(*) from Cart cart join cart.items item where " + predicate;
			assertEquals( 1L, session.createQuery(
					query,
					Long.class
			).setParameter( "item", visibleItem ).getSingleResult() );
			assertEquals( 0L, session.createQuery(
					query,
					Long.class
			).setParameter( "item", restrictedItem ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"items", "unrestrictedItems"})
	void joinWithOnPredicateBindsIdentifierForSingletonInParameter(
			String collection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item restrictedItem = session.find( Item.class, 2L );
			final String query = "select count(*) from Cart cart join cart." + collection
								+ " item on item.id = 1 where item in :items";
			assertEquals( 1L, session.createQuery(
					query,
					Long.class
			).setParameter( "items", List.of( visibleItem ) ).getSingleResult() );
			assertEquals( 0L, session.createQuery(
					query,
					Long.class
			).setParameter( "items", List.of( restrictedItem ) ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"items", "unrestrictedItems"})
	void joinWithOnPredicateBindsIdentifierForSingletonNotInParameter(
			String collection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item restrictedItem = session.find( Item.class, 2L );
			final String query = "select count(*) from Cart cart join cart." + collection
								+ " item on item.id = 1 where item not in :items";
			assertEquals( 0L, session.createQuery(
					query,
					Long.class
			).setParameter( "items", List.of( visibleItem ) ).getSingleResult() );
			assertEquals( 1L, session.createQuery(
					query,
					Long.class
			).setParameter( "items", List.of( restrictedItem ) ).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"items,inner", "items,left", "items,right",
			"unrestrictedItems,inner", "unrestrictedItems,left", "unrestrictedItems,right",
			"manyToManyItems,inner", "manyToManyItems,left", "manyToManyItems,right"
	})
	void joinWithOnPredicateBindsIdentifiersForExpandedInParameter(
			String collection, String joinType, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item unassociatedItem = session.find( Item.class, 5L );
			final List<Item> items = List.of( visibleItem, unassociatedItem );
			final boolean rightJoin = "right".equals( joinType );
			final List<Long> expectedIn = rightJoin ? List.of( 1L, 5L ) : List.of( 1L );
			final List<Long> expectedNotIn = rightJoin ? List.of( 2L, 3L, 4L ) : List.of();
			final String query = "select item.id from Cart cart " + joinType + " join cart." + collection
								+ " item on item.id = 1";
			assertEquals( expectedIn,
					session.createQuery(
									query + " where item in :items order by item.id",
									Long.class
							)
							.setParameter( "items", items )
							.getResultList() );
			assertEquals( expectedNotIn,
					session.createQuery(
									query + " where item not in :items order by item.id",
									Long.class
							)
							.setParameter( "items", items )
							.getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"inner", "left", "right"})
	void joinWithOnPredicateBindsIdentifiersForIndividualInParameters(
			String joinType, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item unassociatedItem = session.find( Item.class, 5L );
			final boolean rightJoin = "right".equals( joinType );
			final List<Long> expectedIn = rightJoin ? List.of( 1L, 5L ) : List.of( 1L );
			final List<Long> expectedNotIn = rightJoin ? List.of( 2L, 3L, 4L ) : List.of();
			final String query = "select item.id from Cart cart " + joinType
								+ " join cart.items item on item.id = 1";
			assertEquals( expectedIn,
					session.createQuery(
									query + " where item in (:visible, :unassociated) order by item.id",
									Long.class
							)
							.setParameter( "visible", visibleItem )
							.setParameter( "unassociated", unassociatedItem )
							.getResultList() );
			assertEquals( expectedNotIn,
					session.createQuery(
									query + " where item not in (:visible, :unassociated) order by item.id",
									Long.class
							)
							.setParameter( "visible", visibleItem )
							.setParameter( "unassociated", unassociatedItem )
							.getResultList() );
		} );
	}

	@ParameterizedTest
	@CsvSource({"items,in", "items,not in", "unrestrictedItems,in", "unrestrictedItems,not in"})
	void rightJoinBindsIdentifiersForInParameter(
			String collection, String operator, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item unassociatedItem = session.find( Item.class, 5L );
			final List<Item> items = List.of( visibleItem, unassociatedItem );
			final List<Long> expected = "in".equals( operator ) ? List.of( 1L, 5L ) : List.of( 2L, 3L, 4L );
			assertEquals( expected,
					session.createQuery(
									"select item.id from Cart cart right join cart." + collection
									+ " item where item " + operator + " :items order by item.id",
									Long.class
							)
							.setParameter( "items", items )
							.getResultList() );
		} );
	}

	@Test
	void rightJoinPreservesRestrictedAndUnassociatedItems(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 5L, session.createQuery(
					"select count(item) from Cart cart right join cart.pkItems item",
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = other", "other = item"})
	void rightJoinEntityComparisonsPreserveRestrictedAndUnassociatedItems(
			String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 5L, session.createQuery(
					"select count(*) from Cart cart right join cart.pkItems item, Item other where " + comparison,
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"items,unrestrictedItems", "pkItems,unrestrictedPkItems",
			"manyToManyItems,unrestrictedItems", "manyToManyPkItems,unrestrictedPkItems"
	})
	void collectionSizeExcludesRestrictedItems(
			String restrictedCollection,
			String unrestrictedCollection,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1, session.createQuery(
					"select size(cart." + restrictedCollection + ") from Cart cart where cart.id = 1",
					Integer.class
			).getSingleResult() );
			assertEquals( 2, session.createQuery(
					"select size(cart." + unrestrictedCollection + ") from Cart cart where cart.id = 1",
					Integer.class
			).getSingleResult() );

			assertEquals( 0, session.createQuery(
					"select size(cart." + restrictedCollection + ") from Cart cart where cart.id = 2",
					Integer.class
			).getSingleResult() );
			assertEquals( 2, session.createQuery(
					"select size(cart." + unrestrictedCollection + ") from Cart cart where cart.id = 2",
					Integer.class
			).getSingleResult() );

			assertEquals( 0, session.createQuery(
					"select size(cart." + restrictedCollection + ") from Cart cart where cart.id = 3",
					Integer.class
			).getSingleResult() );
			assertEquals( 0, session.createQuery(
					"select size(cart." + unrestrictedCollection + ") from Cart cart where cart.id = 3",
					Integer.class
			).getSingleResult() );
		} );
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		public long id;

		@Column(unique = true)
		public String code;
	}

	@Entity(name = "Cart")
	public static class Cart {
		@Id
		public long id;

		@OneToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code")
		)
		public Set<Item> unrestrictedItems = new HashSet<>();

		@OneToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(
						name = "item_code", referencedColumnName = "code", insertable = false, updatable = false
				)
		)
		@SQLJoinTableRestriction("item_code = 'A'")
		public Set<Item> items = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(
						name = "item_code", referencedColumnName = "code", insertable = false, updatable = false
				)
		)
		@SQLJoinTableRestriction("item_code = 'A'")
		public Set<Item> manyToManyItems = new HashSet<>();

		@OneToMany
		@JoinTable(
				name = "cart_pk_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_id")
		)
		public Set<Item> unrestrictedPkItems = new HashSet<>();

		@OneToMany
		@JoinTable(
				name = "cart_pk_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(name = "item_id", insertable = false, updatable = false)
		)
		@SQLJoinTableRestriction("item_id = 1")
		public Set<Item> pkItems = new HashSet<>();

		@ManyToMany
		@JoinTable(
				name = "cart_pk_items",
				joinColumns = @JoinColumn(name = "cart_id", insertable = false, updatable = false),
				inverseJoinColumns = @JoinColumn(name = "item_id", insertable = false, updatable = false)
		)
		@SQLJoinTableRestriction("item_id = 1")
		public Set<Item> manyToManyPkItems = new HashSet<>();
	}
}
