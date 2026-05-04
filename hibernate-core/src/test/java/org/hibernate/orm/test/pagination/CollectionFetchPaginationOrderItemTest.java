/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.cfg.QuerySettings;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pagination of a bidirectional parent/child fetch join must still page orders
 * rather than the cartesian product of orders and items.
 */
@DomainModel(annotatedClasses = {
		CollectionFetchPaginationOrderItemTest.Order.class,
		CollectionFetchPaginationOrderItemTest.Item.class
})
@ServiceRegistry(settings = @Setting(
		name = QuerySettings.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH,
		value = "true"
))
@SessionFactory(useCollectingStatementInspector = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOffsetInSubquery.class)
public class CollectionFetchPaginationOrderItemTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var order0 = new Order( 0L, "Order 0" );
			order0.addItem( new Item( 0L, "Item 0/0" ) );
			order0.addItem( new Item( 1L, "Item 0/1" ) );
			order0.addItem( new Item( 2L, "Item 0/2" ) );

			final var order1 = new Order( 1L, "Order 1" );
			order1.addItem( new Item( 10L, "Item 1/0" ) );
			order1.addItem( new Item( 11L, "Item 1/1" ) );
			order1.addItem( new Item( 12L, "Item 1/2" ) );

			final var order2 = new Order( 2L, "Order 2" );
			order2.addItem( new Item( 20L, "Item 2/0" ) );
			order2.addItem( new Item( 21L, "Item 2/1" ) );
			order2.addItem( new Item( 22L, "Item 2/2" ) );

			final var order3 = new Order( 3L, "Order 3" );
			order3.addItem( new Item( 30L, "Item 3/0" ) );
			order3.addItem( new Item( 31L, "Item 3/1" ) );
			order3.addItem( new Item( 32L, "Item 3/2" ) );

			final var order4 = new Order( 4L, "Order 4" );

			session.persist( order0 );
			session.persist( order1 );
			session.persist( order2 );
			session.persist( order3 );
			session.persist( order4 );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void fetchJoinWithMaxResults(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			sql.clear();

			final var orders = session.createSelectionQuery(
					"from Order o join fetch o.items order by o.orderId",
					Order.class
			).setMaxResults( 2 ).list();

			assertEquals( 2, orders.size() );
			assertEquals( 0L, orders.get( 0 ).getOrderId() );
			assertEquals( 1L, orders.get( 1 ).getOrderId() );
			assertEquals( 3, orders.get( 0 ).getItems().size() );
			assertEquals( 3, orders.get( 1 ).getItems().size() );
			assertTrue( orders.get( 0 ).containsItem( 0L ) );
			assertTrue( orders.get( 0 ).containsItem( 1L ) );
			assertTrue( orders.get( 0 ).containsItem( 2L ) );
			assertTrue( orders.get( 1 ).containsItem( 10L ) );
			assertTrue( orders.get( 1 ).containsItem( 11L ) );
			assertTrue( orders.get( 1 ).containsItem( 12L ) );

			assertEquals( 1, sql.getSqlQueries().size() );
			final String generated = normalized( sql.getSqlQueries().get( 0 ) );
			assertTrue( generated.contains( "from (select" ) );
			assertTrue( generated.contains( "item_entity" ) );
			var dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			if ( !(dialect instanceof HSQLDialect) && !(dialect instanceof MariaDBDialect) && !(dialect instanceof OracleDialect) ) {
				final int existsStart = generated.indexOf( "exists(select 1 from item_entity" );
				final int existsWhere = generated.indexOf( " where", existsStart );
				assertTrue( existsStart >= 0 );
				assertTrue( existsWhere > existsStart );
				assertFalse( generated.substring( existsStart, existsWhere ).contains( " join " ) );
			}
		} );
	}

	@Test
	void fetchLeftJoinWithMaxResults(SessionFactoryScope scope) {
		final var sql = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			sql.clear();

			final var orders = session.createSelectionQuery(
					"from Order o left join fetch o.items order by o.orderId",
					Order.class
			).setFirstResult( 3 ).setMaxResults( 2 ).list();

			assertEquals( 2, orders.size() );
			assertEquals( 3L, orders.get( 0 ).getOrderId() );
			assertEquals( 4L, orders.get( 1 ).getOrderId() );
			assertEquals( 3, orders.get( 0 ).getItems().size() );
			assertEquals( 0, orders.get( 1 ).getItems().size() );

			assertEquals( 1, sql.getSqlQueries().size() );
			final String generated = normalized( sql.getSqlQueries().get( 0 ) );
			assertTrue( generated.contains( "from (select" ) );
			assertTrue( generated.contains( "item_entity" ) );
		} );
	}

	private static String normalized(String sql) {
		return sql.toLowerCase( Locale.ROOT ).replaceAll( "\\s+", " " );
	}

	@Entity(name = "Order")
	@Table(name = "order_entity")
	public static class Order {
		@Id
		private Long orderId;
		private String name;

		@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
		private Set<Item> items = new HashSet<>();

		public Order() {
		}

		public Order(Long orderId, String name) {
			this.orderId = orderId;
			this.name = name;
		}

		public Long getOrderId() {
			return orderId;
		}

		public Set<Item> getItems() {
			return items;
		}

		public void addItem(Item item) {
			items.add( item );
			item.order = this;
		}

		public boolean containsItem(Long itemId) {
			return items.stream().anyMatch( item -> item.itemId.equals( itemId ) );
		}
	}

	@Entity(name = "Item")
	@Table(name = "item_entity")
	public static class Item {
		@Id
		private Long itemId;
		private String name;

		@ManyToOne
		@JoinColumn(name = "order_id")
		private Order order;

		public Item() {
		}

		public Item(Long itemId, String name) {
			this.itemId = itemId;
			this.name = name;
		}
	}
}
