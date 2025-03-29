/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.subselect;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;

import org.hibernate.query.Query;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.annotations.FetchMode.SUBSELECT;

/**
 * Tests sub-select fetching when we have a collection defined with sub-select fetching whose
 * entity-valued element also contains a collection defined with sub-select fetching.
 *
 * Like {@link SimpleMultipleEagerSubSelectFetchTests} and {@link SimpleMultipleLazySubSelectFetchTests},
 * we are trying to make sure that the BatchFetchQueue is not cleared prematurely.  Here we also want to make
 * sure that the proper "nested" restriction gets used.  I.e.
 *
 * ```
 * select ...
 * from line_items l
 * where l.order_fk in (
 *     select o.id
 *     from orders o
 *     where o.customer_fk in (
 *         select c.id
 *         from customers c
 *         where <original-restriction>
 *     )
 * )
 * ```
 */
@DomainModel( annotatedClasses = {
		SimpleNestedSubSelectFetchTests.Customer.class,
		SimpleNestedSubSelectFetchTests.LineItem.class,
		SimpleNestedSubSelectFetchTests.Order.class
})
@SessionFactory( useCollectingStatementInspector = true )
public class SimpleNestedSubSelectFetchTests {

	@Test
	public void simpleTest(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( (session) -> {
			final Query<Customer> query = session.createQuery( "from Customer c where c.name = 'John' or c.name = 'Sally' order by c.id", Customer.class );
			final List<Customer> customers = query.list();

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			statementInspector.clear();

			final Customer john = customers.get( 0 );
			final Customer sally = customers.get( 1 );

			// The 2 customer references should be initialized,
			// but its orders should not
			assertThat( Hibernate.isInitialized( john ) ).isTrue();
			assertThat( Hibernate.isInitialized( sally ) ).isTrue();
			assertThat( Hibernate.isInitialized( john.getOrders() ) ).isFalse();
			assertThat( Hibernate.isInitialized( sally.getOrders() ) ).isFalse();

			// Initialize one of the collection references - both should get initialized
			// in one query
			Hibernate.initialize( john.getOrders() );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );
			statementInspector.clear();

			assertThat( Hibernate.isInitialized( john.getOrders() ) ).isTrue();
			assertThat( Hibernate.isInitialized( sally.getOrders() ) ).isTrue();

			assertThat( john.getOrders() ).hasSize( 2 );
			assertThat( sally.getOrders() ).hasSize( 1 );

			// none of the lineItems should be initialized yet
			john.getOrders().forEach( (order) -> {
				assertThat( Hibernate.isInitialized( order.getLineItems() ) ).isFalse();
			} );
			sally.getOrders().forEach( (order) -> {
				assertThat( Hibernate.isInitialized( order.getLineItems() ) ).isFalse();
			} );

			// initialize one of them
			Hibernate.initialize( john.getOrders().iterator().next().getLineItems() );

			assertThat( statementInspector.getSqlQueries() ).hasSize( 1 );

			john.getOrders().forEach( (order) -> {
				assertThat( Hibernate.isInitialized( order.getLineItems() ) ).isTrue();
			} );
			sally.getOrders().forEach( (order) -> {
				assertThat( Hibernate.isInitialized( order.getLineItems() ) ).isTrue();
			} );
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Customer john = new Customer( 1, "John" );
			final Customer sally = new Customer( 2, "Sally" );
			final Customer jerry = new Customer( 3, "Jerry" );

			final Order johnFirst = new Order( 10, john );
			johnFirst.getLineItems().add( new LineItem( 100, 1.02, 2, johnFirst,  "abc" ) );

			final Order johnSecond = new Order( 11, john );
			johnSecond.getLineItems().add( new LineItem( 110, 1.02, 2, johnSecond,  "abc" ) );
			johnSecond.getLineItems().add( new LineItem( 111, 2.18, 1, johnSecond,  "xyz" ) );
			johnSecond.getLineItems().add( new LineItem( 112, 1.02, 6, johnSecond,  "cba" ) );

			final Order sallyFirst = new Order( 20, sally );
			sallyFirst.getLineItems().add( new LineItem( 200, 1.02, 2, sallyFirst,  "abc" ) );

			final Order jerryFirst = new Order( 30, jerry );
			jerryFirst.getLineItems().add( new LineItem( 300, 1.02, 5, jerryFirst,  "abc" ) );

			session.persist( john );
			session.persist( sally );
			session.persist( jerry );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.remove( session.getReference( Customer.class, 1 ) );
			session.remove( session.getReference( Customer.class, 2 ) );
			session.remove( session.getReference( Customer.class, 3 ) );
		} );
	}

	@Entity( name = "Customer" )
	@Table( name = "customers" )
	public static class Customer {
		@Id
		private Integer id;
		private String name;
		@OneToMany( mappedBy = "customer", fetch = LAZY, cascade = CascadeType.ALL )
		@Fetch( SUBSELECT )
		private Set<Order> orders = new HashSet<>();

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Order> getOrders() {
			return orders;
		}

		public void setOrders(Set<Order> orders) {
			this.orders = orders;
		}
	}

	@Entity( name = "Order" )
	@Table( name = "orders" )
	public static class Order {
		@Id
		private Integer id;
		@CreationTimestamp
		private Instant orderTimestamp;

		@ManyToOne
		@JoinColumn( name = "customer_fk" )
		private Customer customer;

		@OneToMany( mappedBy = "order", fetch = LAZY, cascade = CascadeType.ALL)
		@Fetch( SUBSELECT )
		private List<LineItem> lineItems = new ArrayList<>();

		private Order() {
		}

		public Order(Integer id, Customer customer) {
			this.id = id;
			this.customer = customer;
			customer.getOrders().add( this );
		}

		public Integer getId() {
			return id;
		}

		public Customer getCustomer() {
			return customer;
		}

		public void setCustomer(Customer customer) {
			this.customer = customer;
		}

		public List<LineItem> getLineItems() {
			return lineItems;
		}

		public void setLineItems(List<LineItem> lineItems) {
			this.lineItems = lineItems;
		}
	}

	@Entity( name = "LineItem" )
	@Table( name = "line_items" )
	public static class LineItem {
		@Id
		private Integer id;
		private String productSku;
		private double unitCost;
		private int units;

		@ManyToOne
		@JoinColumn( name = "order_fk" )
		private Order order;

		private LineItem() {
		}

		public LineItem(Integer id, double unitCost, int units, Order order, String productSku) {
			this.id = id;
			this.unitCost = unitCost;
			this.units = units;
			this.order = order;
			this.productSku = productSku;
		}

		public Integer getId() {
			return id;
		}

		public double getUnitCost() {
			return unitCost;
		}

		public void setUnitCost(double unitCost) {
			this.unitCost = unitCost;
		}

		public int getUnits() {
			return units;
		}

		public void setUnits(int units) {
			this.units = units;
		}

		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		public String getProductSku() {
			return productSku;
		}

		public void setProductSku(String productSku) {
			this.productSku = productSku;
		}
	}
}
