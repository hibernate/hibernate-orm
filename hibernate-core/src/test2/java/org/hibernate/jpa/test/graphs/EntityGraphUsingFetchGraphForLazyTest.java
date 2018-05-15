/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Subgraph;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Vlad Mihalcea
 */
public class EntityGraphUsingFetchGraphForLazyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CustomerOrder.class, OrderPosition.class, Product.class, Address.class};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10179" )
	@FailureExpected( jiraKey = "HHH-10179" )
	public void testFetchLazyWithGraphsSubsequently() {
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

		doInJPA( this::entityManagerFactory, em-> {
			em.persist(address);
			em.persist( product );

			em.persist( orderPosition );
			em.persist( customerOrder );
		} );

		doInJPA( this::entityManagerFactory, em-> {
			// First, load with graph on shippingAddress
			EntityGraph<CustomerOrder> addressGraph = em.createEntityGraph( CustomerOrder.class );
			addressGraph.addAttributeNodes( "shippingAddress" );

			Map<String, Object> properties = new HashMap<>();
			properties.put( "javax.persistence.fetchgraph", addressGraph );

			CustomerOrder _customerOrder = em.find( CustomerOrder.class, customerOrder.id, properties );

			assertTrue( Hibernate.isInitialized( _customerOrder ) );
			assertTrue( Hibernate.isInitialized( _customerOrder.shippingAddress ) );
			assertFalse( Hibernate.isInitialized( _customerOrder.orderPosition ) );
			assertFalse( _customerOrder.orderPosition.product != null && Hibernate.isInitialized( _customerOrder.orderPosition.product ) );

			// Second, load with graph on shippingAddress and orderPosition
			EntityGraph<CustomerOrder> addressAndPositionGraph = em.createEntityGraph( CustomerOrder.class );
			addressAndPositionGraph.addAttributeNodes( "shippingAddress" );
			addressAndPositionGraph.addAttributeNodes( "orderPosition" );

			properties = new HashMap<>();
			properties.put( "javax.persistence.fetchgraph", addressAndPositionGraph );

			_customerOrder = em.find( CustomerOrder.class, customerOrder.id, properties );

			assertTrue( Hibernate.isInitialized( _customerOrder ) );
			assertTrue( Hibernate.isInitialized( _customerOrder.shippingAddress ) );
			assertTrue( Hibernate.isInitialized( _customerOrder.orderPosition ) );
			assertFalse( _customerOrder.orderPosition.product != null && Hibernate.isInitialized( _customerOrder.orderPosition.product ) );

			// Third, load with graph on address, orderPosition, and orderPosition.product
			EntityGraph<CustomerOrder> addressAndPositionAndProductGraph = em.createEntityGraph( CustomerOrder.class );
			addressAndPositionAndProductGraph.addAttributeNodes( "shippingAddress" );
			addressAndPositionAndProductGraph.addAttributeNodes( "orderPosition" );
			addressAndPositionAndProductGraph
					.addSubgraph( "orderPosition", OrderPosition.class )
					.addAttributeNodes( "product" );

			properties = new HashMap<>();
			properties.put( "javax.persistence.fetchgraph", addressAndPositionAndProductGraph );

			_customerOrder = em.find( CustomerOrder.class, customerOrder.id, properties );

			assertTrue( Hibernate.isInitialized( _customerOrder ) );
			assertTrue( Hibernate.isInitialized( _customerOrder.shippingAddress ) );
			assertTrue( Hibernate.isInitialized( _customerOrder.orderPosition ) );
			assertTrue( _customerOrder.orderPosition.product != null && Hibernate.isInitialized( _customerOrder.orderPosition.product ) );
		} );
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
