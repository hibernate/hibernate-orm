/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.Hibernate;
import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				EntityGraphAndJoinFetchTest.OrderItem.class,
				EntityGraphAndJoinFetchTest.Order.class,
				EntityGraphAndJoinFetchTest.Product.class
		}
)
@SessionFactory
@JiraKey( value = "HHH-19246")
public class EntityGraphAndJoinFetchTest {

	private static final Long ORDER_ID = 1l;
	private static final Long PRODUCT_ID = 2l;

	private static final String NAMED_GRAPH_NAME = "Order.fetchAll";
	private static final String NAMED_SUBGRAPH_NAME = "OrderItem.fetchAll";


	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = new Order( ORDER_ID, new BigDecimal( 1000 ) );
					Product product = new Product( PRODUCT_ID, "laptop" );
					OrderItem orderItem = new OrderItem( product, order );
					order.getItems().add( orderItem );

					session.persist( order );
					session.persist( product );
					session.persist( orderItem );
				}
		);
	}

	@Test
	public void testJoinFetchBeingSubsetOfGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = session.createQuery( "FROM Order e LEFT JOIN FETCH e.items", Order.class )
							.setHint(
									SpecHints.HINT_SPEC_LOAD_GRAPH,
									scope.getSessionFactory().createEntityManager().getEntityGraph( NAMED_GRAPH_NAME )
							)
							.getSingleResult();
					assertTrue( Hibernate.isInitialized( order.getItems() ), "OrderItems have not been fetched" );
					assertEquals( 1, order.getItems().size(), "OrderItems have not been fetched" );
					assertTrue( Hibernate.isInitialized( order.getItems().iterator().next().getProduct() ), "Product has not been fetched" );
				}
		);
	}

	@Entity(name = "OrderItem")
	@IdClass(OrderItem.PK.class)
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
	@NamedEntityGraph(
			name = NAMED_GRAPH_NAME,
			attributeNodes = {
					@NamedAttributeNode(value = "items", subgraph = NAMED_SUBGRAPH_NAME)
			},
			subgraphs = {
					@NamedSubgraph(name = NAMED_SUBGRAPH_NAME, attributeNodes = {@NamedAttributeNode("product")})
			}
	)
	public static class Order {

		@Id
		private Long id;

		private BigDecimal total;
		@OneToMany(mappedBy = "order")
		private Set<OrderItem> items = new HashSet<>();

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

		public Set<OrderItem> getItems() {
			return items;
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
