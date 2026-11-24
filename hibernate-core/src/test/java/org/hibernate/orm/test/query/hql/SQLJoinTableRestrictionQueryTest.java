package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.SQLJoinTableRestriction;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(annotatedClasses = {
		SQLJoinTableRestrictionQueryTest.Cart.class,
		SQLJoinTableRestrictionQueryTest.Item.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18996")
@Jira("https://hibernate.atlassian.net/browse/HHH-21257")
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
			visibleItem.alternateCode = "V";
			final Item restrictedItem = new Item();
			restrictedItem.id = 2;
			restrictedItem.code = "B";
			restrictedItem.alternateCode = "W";
			final Item anotherRestrictedItem = new Item();
			anotherRestrictedItem.id = 3;
			anotherRestrictedItem.code = "C";
			anotherRestrictedItem.alternateCode = "X";
			final Item additionalRestrictedItem = new Item();
			additionalRestrictedItem.id = 4;
			additionalRestrictedItem.code = "D";
			additionalRestrictedItem.alternateCode = "Y";
			final Item unassociatedItem = new Item();
			unassociatedItem.id = 5;
			unassociatedItem.code = "E";
			unassociatedItem.alternateCode = "Z";

			cart.unrestrictedItems.add( visibleItem );
			cart.unrestrictedItems.add( restrictedItem );
			cart.unrestrictedAlternateKeyItems.add( visibleItem );
			cart.unrestrictedAlternateKeyItems.add( restrictedItem );
			cart.unrestrictedPkItems.add( visibleItem );
			cart.unrestrictedPkItems.add( restrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedItems.add( anotherRestrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedItems.add( additionalRestrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedAlternateKeyItems.add( anotherRestrictedItem );
			cartWithOnlyRestrictedItems.unrestrictedAlternateKeyItems.add( additionalRestrictedItem );
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
	@ValueSource(strings = {"count(item)", "count(*)", "count(cart)"})
	void innerJoinCountsApplyJoinTableFilter(String selection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "itemOneOnly" );
			// Cart 1 holds items 1 and 2, and the filter hides item 2.
			assertEquals( 1L, session.createQuery(
					"select " + selection + " from Cart cart join cart.filteredPkItems item where cart.id = 1",
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"cart.id", "item.id"})
	void innerJoinIdSelectionAppliesJoinTableFilter(String selection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "itemOneOnly" );
			assertEquals( List.of( 1L ), session.createQuery(
					"select " + selection + " from Cart cart join cart.filteredPkItems item where cart.id = 1",
					Long.class
			).getResultList() );
		} );
	}

	@Test
	void innerJoinWithTargetColumnAppliesJoinTableFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "itemOneOnly" );
			assertEquals( List.of( "A" ), session.createQuery(
					"select item.code from Cart cart join cart.filteredPkItems item where cart.id = 1",
					String.class
			).getResultList() );
		} );
	}

	@ParameterizedTest
	@CsvSource({"1,1", "2,0", "3,0"})
	void leftJoinAppliesJoinTableFilter(long cartId, long expectedItems, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "itemOneOnly" );
			final Object[] counts = session.createQuery(
					"select count(item), count(cart) from Cart cart left join cart.filteredPkItems item "
					+ "where cart.id = :id",
					Object[].class
			).setParameter( "id", cartId ).getSingleResult();
			assertEquals( expectedItems, counts[0] );
			assertEquals( 1L, counts[1] );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"unrestrictedItems"})
	void rightJoinPreservesUnassociatedItems(String collection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "itemOneOnly" );
			assertEquals( List.of( 1L, 2L, 3L, 4L, 5L ), session.createQuery(
					"select item.id from Cart cart right join cart." + collection
					+ " item order by item.id",
					Long.class
			).getResultList() );
			assertNull( session.createQuery(
					"select cart.id from Cart cart right join cart." + collection + " item where item.id = 5",
					Long.class
			).getSingleResult() );
		} );
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
	void collectionElementEqualityAppliesRestriction(String predicate, SessionFactoryScope scope) {
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
	void collectionElementInequalityAppliesRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1L, session.createQuery(
					"select count(cart) from Cart cart, Item i "
					+ "where element(cart.items) <> i and i.id = 2",
					Long.class
			).getSingleResult() );
		} );
	}

	@Test
	void collectionElementEqualityAppliesRestrictionToCartIdSelection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( List.of( 1L ), session.createQuery(
					"select cart.id from Cart cart, Item i "
					+ "where element(cart.items) = i and i.code = 'A'",
					Long.class
			).getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = other", "other = item"})
	void entityComparisonWithJoinPredicateOnUniqueKeyJoinTable(String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 1L, session.createQuery(
					"select count(*) from Cart cart join cart.unrestrictedItems item on item.id = 1, Item other where "
					+ comparison,
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = other", "other = item"})
	void rightJoinEntityComparisonOnUniqueKeyJoinTable(String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 5L, session.createQuery(
					"select count(*) from Cart cart right join cart.unrestrictedItems item, Item other where "
					+ comparison,
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = other", "other = item", "item in (other)", "other in (item)"})
	void rightJoinEntityComparisonsPreserveUnassociatedItems(String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String fromClause = " from Cart cart right join cart.unrestrictedItems item, Item other where "
					+ comparison;
			assertEquals( List.of( 1L, 5L ), session.createQuery(
					"select other.id" + fromClause + " and other.id in (1, 5) order by other.id",
					Long.class
			).getResultList() );
			assertNull( session.createQuery(
					"select cart.id" + fromClause + " and other.id = 5",
					Long.class
			).getSingleResult() );
			assertEquals( 1L, session.createQuery(
					"select cart.id" + fromClause + " and other.id = 2",
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"items,item = other", "items,other = item",
			"items,item in (other)", "items,other in (item)",
			"unrestrictedItems,item = other", "unrestrictedItems,other = item",
			"unrestrictedItems,item in (other)", "unrestrictedItems,other in (item)"
	})
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void leftJoinWithOnPredicateEntityComparisonsUseIdentifier(
			String collection, String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final String query = "select other.id from Cart cart left join cart." + collection
					+ " item on item.id = 1, Item other where cart.id = :id and " + comparison
					+ " order by other.id";
			assertEquals( List.of( 1L ), session.createQuery( query, Long.class )
					.setParameter( "id", 1L ).getResultList() );
			assertEquals( List.of(), session.createQuery( query, Long.class )
					.setParameter( "id", 2L ).getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = other", "other = item", "item in (other)", "other in (item)"})
	void entityComparisonsApplyRestrictionInBothDirections(String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( List.of( 1L ), session.createQuery(
					"select other.id from Cart cart join cart.items item, Item other where "
					+ comparison + " order by other.id",
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

	@Test
	void entityInSubqueryOnUniqueKeyJoinTable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 2L, session.createQuery(
					"select count(*) from Cart cart join cart.unrestrictedItems item "
					+ "where cart.id = 1 and item in (select other from Item other)",
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {"item = other", "other = item"})
	void entityComparisonBetweenPrimaryAndUniqueKeyJoinTables(String comparison, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 2L, session.createQuery(
					"select count(*) from Cart cart join cart.unrestrictedItems item "
					+ "join cart.unrestrictedPkItems other where cart.id = 1 and " + comparison,
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"unrestrictedItems,unrestrictedPkItems",
			"unrestrictedPkItems,unrestrictedItems"
	})
	void entityInSubqueryBetweenPrimaryAndUniqueKeyJoinTables(
			String collection, String subqueryCollection, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertEquals( 2L, session.createQuery(
					"select count(*) from Cart cart join cart." + collection + " item "
					+ "where cart.id = 1 and item in (select other from cart." + subqueryCollection + " other)",
					Long.class
			).getSingleResult() );
		} );
	}

	@ParameterizedTest
	@CsvSource({
			"'item in (cart.single, other)',1",
			"'item between other and cart.single',1",
			"'item not between other and cart.single',2"
	})
	void entityPredicatesSupportDifferentAssociationKeys(String predicate, long expectedId, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( Cart.class, 1L ).single = session.find( Item.class, 1L );
			assertEquals( List.of( expectedId ), session.createQuery(
					"select item.id from Cart cart join cart.unrestrictedAlternateKeyItems item "
					+ "join cart.unrestrictedItems other where cart.id = 1 and other.id = 1 and " + predicate,
					Long.class
			).getResultList() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"item in (cart.single, other)",
			"item not in (cart.single, other)"
	})
	void entityInPredicatesSupportDifferentKeysWithMatchingKeyFirst(String predicate, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.find( Cart.class, 1L ).single = session.find( Item.class, 2L );
			assertEquals( predicate.contains( "not in" ) ? List.of() : List.of( 1L, 2L ), session.createQuery(
					"select item.id from Cart cart join cart.unrestrictedItems item "
					+ "join cart.unrestrictedAlternateKeyItems other "
					+ "where cart.id = 1 and other.id = 1 and " + predicate + " order by item.id",
					Long.class
			).getResultList() );
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
	@CsvSource({
			"items,between,1,1",
			"items,not between,0,0",
			"unrestrictedItems,between,1,2",
			"unrestrictedItems,not between,1,0",
			"pkItems,between,1,1",
			"pkItems,not between,0,0",
			"unrestrictedPkItems,between,1,2",
			"unrestrictedPkItems,not between,1,0",
			"manyToManyItems,between,1,1",
			"manyToManyItems,not between,0,0"
	})
	void entityBetweenParametersUseMatchingComparisonKey(
			String collection,
			String operator,
			long expectedSingleCount,
			long expectedRangeCount,
			SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item restrictedItem = session.find( Item.class, 2L );
			final var query = session.createQuery(
					"select count(*) from Cart cart join cart." + collection
					+ " item where cart.id = 1 and item " + operator + " :lower and :upper",
					Long.class
			);
			assertEquals( expectedSingleCount,
					query.setParameter( "lower", visibleItem ).setParameter( "upper", visibleItem ).getSingleResult() );
			assertEquals( expectedRangeCount, query.setParameter( "upper", restrictedItem ).getSingleResult() );
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
	@ValueSource(strings = {"items", "unrestrictedItems", "manyToManyItems"})
	void innerJoinWithOnPredicateBindsIdentifiersForExpandedInParameter(
			String collection, SessionFactoryScope scope) {
		assertExpandedInParameterBindings( collection, "inner", scope );
	}

	@ParameterizedTest
	@ValueSource(strings = {"items", "unrestrictedItems", "manyToManyItems"})
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void leftJoinWithOnPredicateBindsIdentifiersForExpandedInParameter(
			String collection, SessionFactoryScope scope) {
		assertExpandedInParameterBindings( collection, "left", scope );
	}

	private void assertExpandedInParameterBindings(
			String collection, String joinType, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item unassociatedItem = session.find( Item.class, 5L );
			final List<Item> items = List.of( visibleItem, unassociatedItem );
			final String query = "select item.id from Cart cart " + joinType + " join cart." + collection
								+ " item on item.id = 1";
			assertEquals( List.of( 1L ),
					session.createQuery(
									query + " where item in :items order by item.id",
									Long.class
							)
							.setParameter( "items", items )
							.getResultList() );
			assertEquals( List.of(),
					session.createQuery(
									query + " where item not in :items order by item.id",
									Long.class
							)
							.setParameter( "items", items )
							.getResultList() );
		} );
	}

	@Test
	void innerJoinWithOnPredicateBindsIdentifiersForIndividualInParameters(SessionFactoryScope scope) {
		assertIndividualInParameterBindings( "inner", scope );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	void leftJoinWithOnPredicateBindsIdentifiersForIndividualInParameters(SessionFactoryScope scope) {
		assertIndividualInParameterBindings( "left", scope );
	}

	private void assertIndividualInParameterBindings(
			String joinType, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Item visibleItem = session.find( Item.class, 1L );
			final Item unassociatedItem = session.find( Item.class, 5L );
			final String query = "select item.id from Cart cart " + joinType
								+ " join cart.items item on item.id = 1";
			assertEquals( List.of( 1L ),
					session.createQuery(
									query + " where item in (:visible, :unassociated) order by item.id",
									Long.class
							)
							.setParameter( "visible", visibleItem )
							.setParameter( "unassociated", unassociatedItem )
							.getResultList() );
			assertEquals( List.of(),
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
	@ValueSource(strings = {"in", "not in"})
	void unrestrictedRightJoinBindsIdentifiersForInParameter(String operator, SessionFactoryScope scope) {
		assertRightJoinInParameterBindings( "unrestrictedItems", operator, scope );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-21258")
	void restrictedRightJoinBindsIdentifiersForNotInParameter(SessionFactoryScope scope) {
		assertRightJoinInParameterBindings( "items", "not in", scope );
	}

	private void assertRightJoinInParameterBindings(
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

		@Column(unique = true)
		public String alternateCode;
	}

	@Entity(name = "Cart")
	@FilterDef(name = "itemOneOnly")
	public static class Cart {
		@Id
		public long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "single_code", referencedColumnName = "code")
		public Item single;

		@OneToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_code", referencedColumnName = "code")
		)
		public Set<Item> unrestrictedItems = new HashSet<>();

		@OneToMany
		@JoinTable(
				name = "cart_alternate_key_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_alternate_code", referencedColumnName = "alternateCode")
		)
		public Set<Item> unrestrictedAlternateKeyItems = new HashSet<>();

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
		@FilterJoinTable(name = "itemOneOnly", condition = "item_id = 1")
		public Set<Item> filteredPkItems = new HashSet<>();

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
