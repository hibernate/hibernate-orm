/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.nestedfetch;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		NestedFetchTest.Product.class,
		NestedFetchTest.Customer.class,
		NestedFetchTest.Order.class,
		NestedFetchTest.OrderLine.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16905" )
public class NestedFetchTest {
	@BeforeAll
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Product sprocket = new Product( 1, "Sprocket" );
			final Product thingamajig = new Product( 2, "Thingamajig" );
			session.persist( sprocket );
			session.persist( thingamajig );

			final Customer ibm = new Customer( 1, "IBM" );
			session.persist( ibm );

			final Order order = new Order( 1, "ibm-1", ibm );
			order.addOrderLine( sprocket, 20 );
			order.addOrderLine( thingamajig, 1500 );
			session.persist( order );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testNestedFetch(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();
		sqlCollector.clear();

		final Customer customer = scope.fromTransaction( (session) -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Customer> criteria = cb.createQuery( Customer.class );
			final Root<Customer> fromRoot = criteria.from( Customer.class );
			final Fetch<Customer, Order> ordersFetch = fromRoot.fetch( "orders", JoinType.LEFT );
			final Fetch<Order, OrderLine> linesFetch = ordersFetch.fetch( "lines", JoinType.LEFT );
			final Fetch<OrderLine, Product> productFetch = linesFetch.fetch( "product", JoinType.LEFT );
			return session.createQuery( criteria.select( fromRoot ) ).getSingleResult();
		} );

		// make sure that the fetches really got fetched...
		assertThat( customer.orders ).hasSize( 1 );
		final Order order = customer.orders.iterator().next();
		assertThat( order.lines ).hasSize( 2 );
		assertThat( order.lines.stream().map( orderLine -> orderLine.product.name ) )
				.contains( "Sprocket", "Thingamajig" );

		// should have happened by joins
		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
	}

	@Entity
	@Table(name="products")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Product {
		@Id
		private Integer id;
		private String name;

		public Product() {
		}

		public Product(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	@Table(name="customers")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Customer {
		@Id
		private Integer id;
		private String name;
		@OneToMany( mappedBy = "customer" )
		private Set<Order> orders = new HashSet<>();

		public Customer() {
		}

		public Customer(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity
	@Table(name="orders")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class Order {
		@Id
		private Integer id;
		private String orderNumber;
		@ManyToOne
		@JoinColumn(name = "customer_fk")
		private Customer customer;
		@OneToMany( mappedBy = "order", cascade = CascadeType.ALL)
		private Set<OrderLine> lines = new HashSet<>();

		public Order() {
		}

		public Order(Integer id, String orderNumber, Customer customer) {
			this.id = id;
			this.orderNumber = orderNumber;
			this.customer = customer;
			customer.orders.add( this );
		}

		public void addOrderLine(Product product, int quantity) {
			lines.add( new OrderLine( this, product, quantity ) );
		}
	}

	@Entity(name="OrderLine")
	@Table(name="OrderLine")
	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	public static class OrderLine {
		@Id
		@GeneratedValue
		private Integer id;
		@ManyToOne
		@JoinColumn(name="order_fk")
		private Order order;
		@ManyToOne
		@JoinColumn(name="product_fk")
		private Product product;
		private int quantity;

		public OrderLine() {
		}

		public OrderLine(Order order, Product product, int quantity) {
			this.order = order;
			this.product = product;
			this.quantity = quantity;
		}
	}
}
