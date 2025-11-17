/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				LoadEntityWithIdClassEntityGraphTest.OrderItem.class,
				LoadEntityWithIdClassEntityGraphTest.Order.class,
				LoadEntityWithIdClassEntityGraphTest.Product.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-15674")
public class LoadEntityWithIdClassEntityGraphTest {

	private static final Long ORDER_ID = 1l;
	private static final Long PRODUCT_ID = 2l;

	private static final String NAMED_GRAPH_NAME = "OrderItem.fetchAll";


	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = new Order( ORDER_ID, new BigDecimal( 1000 ) );
					Product product = new Product( PRODUCT_ID, "laptop" );
					OrderItem orderItem = new OrderItem( product, order );

					session.persist( order );
					session.persist( product );
					session.persist( orderItem );
				}
		);
	}

	@Test
	public void testFetchGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					OrderItem orderItem = session.createQuery(
									"SELECT e FROM OrderItem e WHERE e.order.id = :orderId AND e.product.id = :productId",
									OrderItem.class
							)
							.setParameter( "orderId", ORDER_ID )
							.setParameter( "productId", PRODUCT_ID )
							.setHint(
									SpecHints.HINT_SPEC_FETCH_GRAPH,
									scope.getSessionFactory().createEntityManager().getEntityGraph( NAMED_GRAPH_NAME )
							)
							.getSingleResult();
					assertTrue( Hibernate.isInitialized( orderItem.getOrder() ), "Order has not been fetched" );
					assertTrue( Hibernate.isInitialized( orderItem.getProduct() ), "Product has not been fetched" );
				}
		);
	}

	@Entity(name = "OrderItem")
	@IdClass(OrderItem.PK.class)
	@NamedEntityGraph(
			name = NAMED_GRAPH_NAME,
			attributeNodes = {
					@NamedAttributeNode("product"),
					@NamedAttributeNode("order"),
			}
	)
	public static class OrderItem {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "product_id")
		private Product product;

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "order_id")
		private Order order;

		public OrderItem() {
		}

		public OrderItem(Product product, Order order) {
			this.product = product;
			this.order = order;
		}

		public Product getProduct() {
			return product;
		}

		public Order getOrder() {
			return order;
		}

		public static class PK implements Serializable {
			private Long product;
			private Long order;

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( product, pk.product ) && Objects.equals( order, pk.order );
			}

			@Override
			public int hashCode() {
				return Objects.hash( product, order );
			}
		}
	}


	@Entity(name = "Order")
	@Table(name = "ORDER_TABLE")
	public static class Order {

		@Id
		private Long id;

		private BigDecimal total;

		public Order() {
		}

		public Order(Long id, BigDecimal total) {
			this.id = id;
			this.total = total;
		}

		public Long getId() {
			return id;
		}

		public BigDecimal getTotal() {
			return total;
		}

	}

	@Entity(name = "Product")
	public static class Product {

		@Id
		private Long id;

		private String name;

		public Product() {
		}

		public Product(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

	}
}
