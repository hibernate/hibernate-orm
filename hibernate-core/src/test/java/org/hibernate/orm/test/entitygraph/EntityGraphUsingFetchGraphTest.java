/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Subgraph;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.TypedQuery;

import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;

/**
 * @author Baris Cubukcuoglu
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphUsingFetchGraphTest.CustomerOrder.class,
				EntityGraphUsingFetchGraphTest.OrderPosition.class,
				EntityGraphUsingFetchGraphTest.Product.class,
				EntityGraphUsingFetchGraphTest.Address.class
		}
)
@SessionFactory
public class EntityGraphUsingFetchGraphTest {

	@Test
	@JiraKey( value = "HHH-9392")
	void fetchSubGraphFromSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = new Address();
					address.city = "TestCity";

					CustomerOrder customerOrder = new CustomerOrder();
					customerOrder.shippingAddress = address;

					Product product = new Product();

					OrderPosition orderPosition = new OrderPosition();
					orderPosition.product = product;

					customerOrder.orderPosition = orderPosition;
					session.persist( address );
					session.persist( orderPosition );
					session.persist( product );
					session.persist( customerOrder );
				}
		);

		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					final EntityGraph<CustomerOrder> entityGraph = em.createEntityGraph( CustomerOrder.class );
					//entityGraph.addAttributeNodes( "shippingAddress", "orderDate" );
					entityGraph.addAttributeNodes( "shippingAddress" );

					final Subgraph<OrderPosition> orderProductsSubgraph = entityGraph.addSubgraph( "orderPosition" );
					//orderProductsSubgraph.addAttributeNodes( "amount" );

					final Subgraph<Product> productSubgraph = orderProductsSubgraph.addSubgraph( "product" );
					//productSubgraph.addAttributeNodes( "productName" );

					TypedQuery<CustomerOrder> query = em.createQuery(
							"SELECT o FROM CustomerOrder o", CustomerOrder.class
					);
					query.setHint( GraphSemantic.LOAD.getJpaHintName(), entityGraph );
					final List<CustomerOrder> results = query.getResultList();

					assertThat( results, isInitialized() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9392")
	void fetchAttributeNodeByStringFromSubgraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Address address = new Address();
					address.city = "TestCity";

					CustomerOrder customerOrder = new CustomerOrder();
					customerOrder.shippingAddress = address;

					Product product = new Product();

					OrderPosition orderPosition = new OrderPosition();
					orderPosition.product = product;

					customerOrder.orderPosition = orderPosition;
					session.persist( address );
					session.persist( orderPosition );
					session.persist( product );
					session.persist( customerOrder );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityManager em = session.unwrap( EntityManager.class );
					final EntityGraph<CustomerOrder> entityGraph = em.createEntityGraph( CustomerOrder.class );
					entityGraph.addAttributeNodes( "shippingAddress", "orderDate" );
					entityGraph.addAttributeNodes( "shippingAddress" );

					final Subgraph<OrderPosition> orderProductsSubgraph = entityGraph.addSubgraph( "orderPosition" );
					orderProductsSubgraph.addAttributeNodes( "amount" );
					orderProductsSubgraph.addAttributeNodes( "product" );

					final Subgraph<Product> productSubgraph = orderProductsSubgraph.addSubgraph( "product" );
					productSubgraph.addAttributeNodes( "productName" );

					TypedQuery<CustomerOrder> query = em.createQuery(
							"SELECT o FROM CustomerOrder o", CustomerOrder.class
					);
					query.setHint( GraphSemantic.LOAD.getJpaHintName(), entityGraph );
					final List<CustomerOrder> results = query.getResultList();

					assertThat( results, isInitialized() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-9392")
	void fetchUsingHql(SessionFactoryScope scope) {
		// This test is here only for comparison with results from fetchAttributeNodeFromSubgraph.
		// At the time this was written, the generated SQL from the HQL is the same as that generated with the
		// query hint in fetchAttributeNodeFromSubgraph. I am leaving this here for future debugging purposes.
		scope.inTransaction(
				session -> {
					Address address = new Address();
					address.city = "TestCity";

					CustomerOrder customerOrder = new CustomerOrder();
					customerOrder.shippingAddress = address;

					Product product = new Product();

					OrderPosition orderPosition = new OrderPosition();
					orderPosition.product = product;

					customerOrder.orderPosition = orderPosition;
					session.persist( address );
					session.persist( orderPosition );
					session.persist( product );
					session.persist( customerOrder );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityManager em = session.unwrap( EntityManager.class );
					TypedQuery<CustomerOrder> query = em.createQuery(
							"SELECT o FROM CustomerOrder o left join fetch o.orderPosition pos left join fetch pos.product left join fetch o.shippingAddress", CustomerOrder.class
					);
					final List<CustomerOrder> results = query.getResultList();

					assertThat( results, isInitialized() );
				}
		);
	}

	@Entity(name = "CustomerOrder")
	@Table(name = "customerOrder")
	public static class CustomerOrder {
		@Id
		@GeneratedValue
		public Long id;

		@OneToOne
		public OrderPosition orderPosition;

		@Temporal(TemporalType.TIMESTAMP)
		public Date orderDate;

		@OneToOne
		public Address shippingAddress;
	}

	@Entity(name = "Address")
	@Table(name = "address")
	public static class Address {
		@Id
		@GeneratedValue
		public Long id;

		public String city;
	}

	@Entity(name = "OrderPosition")
	@Table(name = "orderPosition")
	public static class OrderPosition {
		@Id
		@GeneratedValue
		public Long id;

		public Integer amount;

		@ManyToOne
		@JoinColumn(name = "product")
		public Product product;
	}

	@Entity(name = "Product")
	@Table(name = "product")
	public static class Product {
		@Id
		@GeneratedValue
		public Long id;

		public String productName;
	}
}
