/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManyBidirectionalTest.Order.class,
				OneToManyBidirectionalTest.Item.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
public class OneToManyBidirectionalTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Item i = new Item( 1L );
			session.persist( i );

			Item i2 = new Item( 2L );
			session.persist( i2 );

			Order o = new Order( 3L );
			o.addItem( i );
			o.addItem( i2 );

			session.persist( o );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFetchingSameAssociationTwice(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					Statistics statistics = session.getSessionFactory().getStatistics();
					statistics.clear();
					sqlStatementInterceptor.clear();

					List<Item> items = session.createQuery(
							"from Item i" +
									"    join fetch i.order o" +
									"    join fetch i.order", Item.class ).list();

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
					sqlStatementInterceptor.clear();

					assertThat( items.size(), is( 2 ) );
					Order order = items.get( 0 ).getOrder();
					assertTrue( Hibernate.isInitialized( order ) );
					List<Item> lineItems = order.getLineItems();
					assertFalse( Hibernate.isInitialized( lineItems ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
					Item item = lineItems.get( 0 );

					/*
						select l1_0."order_id", l1_0.id
						from Item as l1_0
							where l1_0."order_id" = ?
					 */

					sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
					sqlStatementInterceptor.clear();

					Order itemOrder = item.getOrder();
					assertTrue( Hibernate.isInitialized( itemOrder ) );
					assertThat( itemOrder, is( itemOrder ) );
					assertTrue( Hibernate.isInitialized( itemOrder.getLineItems() ) );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

				}
		);
	}

	@Test
	public void testRetrievingItem(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					Statistics statistics = session.getSessionFactory().getStatistics();
					statistics.clear();
					sqlStatementInterceptor.clear();

					Item item = session.find( Item.class, 1L );

					/*
						select i1_0.id, o1_0.id, o1_0.name
							from Item as i1_0
							left outer join "Order" as o1_0  on i1_0."order_id" = o1_0.id
							where i1_0.id = ?
					 */

					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.LEFT, 1 );
					sqlStatementInterceptor.clear();

					Order order = item.getOrder();
					assertTrue( Hibernate.isInitialized( order ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( order.getLineItems().size(), is( 2 ) );

					/*
						select l1_0."order_id", l1_0.id
						from Item as l1_0
						where l1_0."order_id" = ?
					 */

					sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
					sqlStatementInterceptor.clear();

					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
					for ( Item itm : order.getLineItems() ) {
						assertThat( itm.getOrder(), is( order ) );
					}
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

				} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Item> results = session.createQuery( "select i from Item i", Item.class ).list();
			/*
				1) select i1_0.id, i1_0."order_id"
					from Item as i1_0
				2) 	select o1_0.id, o1_0.name
					from "Order" as o1_0
					where o1_0.id = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 ).getOrder();
			assertTrue( Hibernate.isInitialized( order ) );
			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

			Assert.assertFalse( Hibernate.isInitialized( order.getLineItems() ) );

			assertThat( order.getLineItems().size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertThat( statistics.getPrepareStatementCount(), is( 3L ) );

			for ( Item itm : order.getLineItems() ) {
				assertThat( itm.getOrder(), is( order ) );
			}
			assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Item> results = session.createQuery( "select i from Item i join i.order", Item.class ).list();
			/*
				1)	select i1_0.id, i1_0."order_id"
					from Item as i1_0
					inner join "Order" as o1_0 on i1_0."order_id" = o1_0.id
				2) select o1_0.id, o1_0.name
					from "Order" as o1_0
						where o1_0.id = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
			sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 ).getOrder();
			assertTrue( Hibernate.isInitialized( order ) );
			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

			Assert.assertFalse( Hibernate.isInitialized( order.getLineItems() ) );
			assertThat( order.getLineItems().size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertThat( statistics.getPrepareStatementCount(), is( 3L ) );

			for ( Item itm : order.getLineItems() ) {
				assertThat( itm.getOrder(), is( order ) );
			}
			assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Item> results = session.createQuery( "select i from Item i join fetch i.order", Item.class ).list();

			/*
				select i1_0.id, o1_0.id, o1_0.name
				from Item as i1_0
				inner join "Order" as o1_0 on i1_0."order_id" = o1_0.id
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 ).getOrder();
			assertTrue( Hibernate.isInitialized( order ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			Assert.assertFalse( Hibernate.isInitialized( order.getLineItems() ) );

			assertThat( order.getLineItems().size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

			Assert.assertTrue( Hibernate.isInitialized( order.getLineItems() ) );

			for ( Item itm : order.getLineItems() ) {
				assertThat( itm.getOrder(), is( order ) );
			}
			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Item> results = session.createQuery(
					"select i from Item i join i.order o join o.lineItems",
					Item.class
			).list();

			/*
				1)	select i1_0.id, i1_0."order_id"
					from Item as i1_0
					inner join "Order" as o1_0 on i1_0."order_id" = o1_0.id
					inner join Item as l1_0  on l1_0."order_id" = o1_0.id
				2)	select o1_0.id, o1_0.name
					from "Order" as o1_0
					where o1_0.id = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );
			sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );
			sqlStatementInterceptor.clear();

			Item item = results.get( 0 );
			Order order = item.getOrder();
			assertTrue( Hibernate.isInitialized( order ) );
			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

			Assert.assertFalse( Hibernate.isInitialized( order.getLineItems() ) );
			assertThat( order.getLineItems().size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			for ( Item itm : order.getLineItems() ) {
				assertThat( itm.getOrder(), is( order ) );
			}
			assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
		} );
	}

	@Test
	public void testRetrievingOrder(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			Order order = session.find( Order.class, 3L );

			/*
				select o1_0.id, o1_0.name
				from "Order" as o1_0
				where o1_0.id = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			List<Item> lineItems = order.getLineItems();
			assertFalse( Hibernate.isInitialized( lineItems ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			assertThat( lineItems.size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertTrue( Hibernate.isInitialized( lineItems ) );
			for ( Item itm : lineItems ) {
				assertThat( itm.getOrder(), is( order ) );
			}

			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
		} );


		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Order> results = session.createQuery( "select o from Order o", Order.class ).list();

			/*
				select o1_0.id, o1_0.name
				from "Order" as o1_0
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 );
			List<Item> lineItems = order.getLineItems();
			assertFalse( Hibernate.isInitialized( lineItems ) );

			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			assertThat( lineItems.size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertTrue( Hibernate.isInitialized( lineItems ) );
			for ( Item itm : lineItems ) {
				assertThat( itm.getOrder(), is( order ) );
			}

			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Order> results = session.createQuery( "select o from Order o join o.lineItems", Order.class ).list();

			/*
				select o1_0.id, o1_0.name
				from "Order" as o1_0
				inner join Item as l1_0 on l1_0."order_id" = o1_0.id
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 );
			assertFalse( Hibernate.isInitialized( order.getLineItems() ) );
			List<Item> lineItems = order.getLineItems();
			assertFalse( Hibernate.isInitialized( lineItems ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			assertThat( lineItems.size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertTrue( Hibernate.isInitialized( lineItems ) );

			for ( Item itm : lineItems ) {
				assertThat( itm.getOrder(), is( order ) );
			}
			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Order> results = session.createQuery( "select o from Order o join fetch o.lineItems", Order.class )
					.list();

			/*
				select o1_0.id, l1_0."order_id", l1_0.id, o1_0.name
				from "Order" as o1_0
				inner join Item as l1_0 on l1_0."order_id" = o1_0.id
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 );
			List<Item> lineItems = order.getLineItems();
			assertTrue( Hibernate.isInitialized( lineItems ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			assertThat( lineItems.size(), is( 2 ) );
			for ( Item itm : lineItems ) {
				assertThat( itm.getOrder(), is( order ) );
			}

			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Order> results = session.createQuery(
					"select o from Order o join o.lineItems l join l.order",
					Order.class
			).list();

			/*
				select o1_0.id, l1_0."order_id", l1_0.id, o1_0.name, o2_0.id, o2_0.name
				from "Order" as o1_0
				inner join Item as l1_0 on l1_0."order_id" = o1_0.id
				inner join "Order" as o2_0 on l1_0."order_id" = o2_0.id
			 */

			// todo (6.0): this was originally intended to produce only a single SQL join,
			//  but joins are created before fetches, thus we don't know about bidirectional fetching/joining
			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 );
			List<Item> lineItems = order.getLineItems();
			assertFalse( Hibernate.isInitialized( lineItems ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			assertThat( lineItems.size(), is( 2 ) );

			/*
				select l1_0."order_id", l1_0.id
				from Item as l1_0
				where l1_0."order_id" = ?
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
			sqlStatementInterceptor.clear();

			assertTrue( Hibernate.isInitialized( lineItems ) );
			for ( Item itm : lineItems ) {
				assertThat( itm.getOrder(), is( order ) );
			}
			assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
		} );

		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Order> results = session.createQuery(
					"select o from Order o join fetch o.lineItems l join fetch l.order",
					Order.class
			).list();

			/*
				select o1_0.id, l1_0."order_id", l1_0.id, o1_0.name, o2_0.id, o2_0.name
				from "Order" as o1_0
				inner join Item as l1_0 on l1_0."order_id" = o1_0.id
				inner join "Order" as o2_0 on l1_0."order_id" = o2_0.id
			 */

			// todo (6.0): this was originally intended to produce only a single SQL join,
			//  but joins are created before fetches, thus we don't know about bidirectional fetching/joining
			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 );
			List<Item> lineItems = order.getLineItems();
			assertTrue( Hibernate.isInitialized( lineItems ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

			assertThat( lineItems.size(), is( 2 ) );
			for ( Item itm : lineItems ) {
				assertThat( itm.getOrder(), is( order ) );
			}

			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
		} );
	}

	@Test
	public void testItemFetchJoin(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			Statistics statistics = session.getSessionFactory().getStatistics();
			statistics.clear();
			sqlStatementInterceptor.clear();

			List<Item> results = session.createQuery(
					"select i from Item i join fetch i.order o join fetch o.lineItems",
					Item.class
			).list();

			/*
				select i1_0.id, o1_0.id, l1_0."order_id", l1_0.id, o1_0.name
				from Item as i1_0
				inner join "Order" as o1_0 on i1_0."order_id" = o1_0.id
				inner join Item as l1_0 on l1_0."order_id" = o1_0.id
			 */

			sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );
			sqlStatementInterceptor.clear();

			Order order = results.get( 0 ).getOrder();
			assertTrue( Hibernate.isInitialized( order ) );
			assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
			assertTrue( Hibernate.isInitialized( order.getLineItems() ) );

			// With BAG semantics, the list will contain duplicates, so filter that
			assertThat( new HashSet<>( order.getLineItems()).size(), is( 2 ) );
		} );
	}

	@Test
	public void testItemJoinWithFetchJoin(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		Assertions.assertThrows( IllegalArgumentException.class, () ->
				scope.inTransaction( session -> {
					Statistics statistics = session.getSessionFactory().getStatistics();
					statistics.clear();
					sqlStatementInterceptor.clear();

					List<Item> results = session.createQuery(
							"select i from Item i join i.order o join fetch o.lineItems",
							Item.class
					).list();

				} )
		);
	}

	@Test
	public void testCircularReferenceDetection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List<Order> orders = session.createQuery(
					"select o from Order o join fetch o.lineItems",
					Order.class
			).list();
			orders.forEach(
					order ->
							order.getLineItems().forEach(
									item -> assertThat( item.getOrder(), sameInstance( order ) )
							)
			);
		} );
	}

	@Entity(name = "Order")
	@Table(name = "`Order`")
	public static class Order {
		@Id
		private Long id;

		public Order() {
		}

		public Order(Long id) {
			this.id = id;
		}

		private String name;

		@OneToMany(mappedBy = "order")
		List<Item> lineItems = new ArrayList<>();

		public List<Item> getLineItems() {
			return lineItems;
		}

		public void setLineItems(List<Item> lineItems) {
			this.lineItems = lineItems;
		}

		public void addItem(Item item) {
			this.lineItems.add( item );
			item.setOrder( this );
		}
	}

	@Entity(name = "Item")
	@Table(name = "Item")
	public static class Item {
		@Id
		private Long id;

		public Item() {
		}

		public Item(Long id) {
			this.id = id;
		}

		@ManyToOne
		private Order order;

		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}
	}

}
