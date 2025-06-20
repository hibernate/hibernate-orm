/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {
		EntityGraphUsingFetchGraphForLazyTest.CustomerOrder.class,
		EntityGraphUsingFetchGraphForLazyTest.OrderPosition.class,
		EntityGraphUsingFetchGraphForLazyTest.Product.class,
		EntityGraphUsingFetchGraphForLazyTest.Address.class
})
public class EntityGraphUsingFetchGraphForLazyTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-10179")
	@FailureExpected(jiraKey = "HHH-10179")
	public void testFetchLazyWithGraphsSubsequently(EntityManagerFactoryScope scope) {
		Address address = new Address();
		address.city = "C9";
		Product product = new Product();
		product.productName = "P1";

		OrderPosition orderPosition = new OrderPosition();
		orderPosition.product = product;
		orderPosition.amount = 100;

		CustomerOrder customerOrder = new CustomerOrder();
		customerOrder.orderPosition = orderPosition;
		customerOrder.shippingAddress = address;

		scope.inTransaction(
				entityManager -> {
					entityManager.persist( address );
					entityManager.persist( product );

					entityManager.persist( orderPosition );
					entityManager.persist( customerOrder );
				}
		);

		scope.inTransaction(
				entityManager -> {
					// First, load with graph on shippingAddress
					EntityGraph<CustomerOrder> addressGraph = entityManager.createEntityGraph( CustomerOrder.class );
					addressGraph.addAttributeNodes( "shippingAddress" );

					Map<String, Object> properties = new HashMap<>();
					properties.put( "javax.persistence.fetchgraph", addressGraph );

					CustomerOrder _customerOrder = entityManager.find( CustomerOrder.class, customerOrder.id, properties );

					assertTrue( Hibernate.isInitialized( _customerOrder ) );
					assertTrue( Hibernate.isInitialized( _customerOrder.shippingAddress ) );
					assertFalse( Hibernate.isInitialized( _customerOrder.orderPosition ) );
					assertFalse( _customerOrder.orderPosition.product != null && Hibernate.isInitialized( _customerOrder.orderPosition.product ) );

					// Second, load with graph on shippingAddress and orderPosition
					EntityGraph<CustomerOrder> addressAndPositionGraph = entityManager.createEntityGraph( CustomerOrder.class );
					addressAndPositionGraph.addAttributeNodes( "shippingAddress" );
					addressAndPositionGraph.addAttributeNodes( "orderPosition" );

					properties = new HashMap<>();
					properties.put( "javax.persistence.fetchgraph", addressAndPositionGraph );

					_customerOrder = entityManager.find( CustomerOrder.class, customerOrder.id, properties );

					assertTrue( Hibernate.isInitialized( _customerOrder ) );
					assertTrue( Hibernate.isInitialized( _customerOrder.shippingAddress ) );
					assertTrue( Hibernate.isInitialized( _customerOrder.orderPosition ) );
					assertFalse( _customerOrder.orderPosition.product != null && Hibernate.isInitialized( _customerOrder.orderPosition.product ) );

					// Third, load with graph on address, orderPosition, and orderPosition.product
					EntityGraph<CustomerOrder> addressAndPositionAndProductGraph = entityManager.createEntityGraph( CustomerOrder.class );
					addressAndPositionAndProductGraph.addAttributeNodes( "shippingAddress" );
					addressAndPositionAndProductGraph.addAttributeNodes( "orderPosition" );
					addressAndPositionAndProductGraph
							.addSubgraph( "orderPosition", OrderPosition.class )
							.addAttributeNodes( "product" );

					properties = new HashMap<>();
					properties.put( "javax.persistence.fetchgraph", addressAndPositionAndProductGraph );

					_customerOrder = entityManager.find( CustomerOrder.class, customerOrder.id, properties );

					assertTrue( Hibernate.isInitialized( _customerOrder ) );
					assertTrue( Hibernate.isInitialized( _customerOrder.shippingAddress ) );
					assertTrue( Hibernate.isInitialized( _customerOrder.orderPosition ) );
					assertTrue( _customerOrder.orderPosition.product != null && Hibernate.isInitialized( _customerOrder.orderPosition.product ) );
				}
		);
	}

	@Entity
	@Table(name = "customerOrder")
	public static class CustomerOrder {
		@Id
		@GeneratedValue
		public Long id;

		@OneToOne(fetch = FetchType.LAZY)
		public OrderPosition orderPosition;

		@Temporal(TemporalType.TIMESTAMP)
		public Date orderDate;

		@OneToOne(fetch = FetchType.LAZY)
		public Address shippingAddress;
	}

	@Entity
	@Table(name = "address")
	public static class Address {
		@Id
		@GeneratedValue
		public Long id;

		public String city;
	}

	@Entity
	@Table(name = "orderPosition")
	public static class OrderPosition {
		@Id
		@GeneratedValue
		public Long id;

		public Integer amount;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "product")
		public Product product;
	}

	@Entity
	@Table(name = "product")
	public static class Product {
		@Id
		@GeneratedValue
		public Long id;

		public String productName;
	}
}
