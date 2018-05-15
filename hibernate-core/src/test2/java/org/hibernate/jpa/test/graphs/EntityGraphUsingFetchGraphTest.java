/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Subgraph;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import java.util.Date;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Baris Cubukcuoglu
 */
public class EntityGraphUsingFetchGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {CustomerOrder.class, OrderPosition.class, Product.class, Address.class};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9392")
	public void fetchSubGraphFromSubgraph() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Address address = new Address();
		address.city = "TestCity";

		CustomerOrder customerOrder = new CustomerOrder();
		customerOrder.shippingAddress = address;

		Product product = new Product();

		OrderPosition orderPosition = new OrderPosition();
		orderPosition.product = product;

		customerOrder.orderPosition = orderPosition;
		em.persist( address );
		em.persist( orderPosition );
		em.persist( product );
		em.persist( customerOrder );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		final EntityGraph<CustomerOrder> entityGraph = em.createEntityGraph( CustomerOrder.class );
		//entityGraph.addAttributeNodes( "shippingAddress", "orderDate" );
		entityGraph.addAttributeNodes( "shippingAddress" );

		final Subgraph<OrderPosition> orderProductsSubgraph = entityGraph.addSubgraph( "orderPosition" );
		//orderProductsSubgraph.addAttributeNodes( "amount" );

		final Subgraph<Product> productSubgraph = orderProductsSubgraph.addSubgraph( "product" );
		//productSubgraph.addAttributeNodes( "productName" );

		TypedQuery<CustomerOrder> query = em.createQuery(
				"SELECT o FROM EntityGraphUsingFetchGraphTest$CustomerOrder o", CustomerOrder.class
		);
		query.setHint( "javax.persistence.loadgraph", entityGraph );
		final List<CustomerOrder> results = query.getResultList();

		assertTrue( Hibernate.isInitialized( results ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9392")
	public void fetchAttributeNodeFromSubgraph() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Address address = new Address();
		address.city = "TestCity";

		CustomerOrder customerOrder = new CustomerOrder();
		customerOrder.shippingAddress = address;

		Product product = new Product();

		OrderPosition orderPosition = new OrderPosition();
		orderPosition.product = product;

		customerOrder.orderPosition = orderPosition;
		em.persist( address );
		em.persist( orderPosition );
		em.persist( product );
		em.persist( customerOrder );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		final EntityGraph<CustomerOrder> entityGraph = em.createEntityGraph( CustomerOrder.class );
		entityGraph.addAttributeNodes( "shippingAddress", "orderDate" );
		entityGraph.addAttributeNodes( "shippingAddress" );

		final Subgraph<OrderPosition> orderProductsSubgraph = entityGraph.addSubgraph( "orderPosition" );
		orderProductsSubgraph.addAttributeNodes( "amount" );
		orderProductsSubgraph.addAttributeNodes( "product" );

		final Subgraph<Product> productSubgraph = orderProductsSubgraph.addSubgraph( "product" );
		productSubgraph.addAttributeNodes( "productName" );

		TypedQuery<CustomerOrder> query = em.createQuery(
				"SELECT o FROM EntityGraphUsingFetchGraphTest$CustomerOrder o", CustomerOrder.class
		);
		query.setHint( "javax.persistence.loadgraph", entityGraph );
		final List<CustomerOrder> results = query.getResultList();

		assertTrue( Hibernate.isInitialized( results ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9392")
	public void fetchUsingHql() {
		// This test is here only for comparison with results from fetchAttributeNodeFromSubgraph.
		// At the time this was written, the generated SQL from the HQL is the same as that generated with the
		// query hint in fetchAttributeNodeFromSubgraph. I am leaving this here for future debugging purposes.
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Address address = new Address();
		address.city = "TestCity";

		CustomerOrder customerOrder = new CustomerOrder();
		customerOrder.shippingAddress = address;

		Product product = new Product();

		OrderPosition orderPosition = new OrderPosition();
		orderPosition.product = product;

		customerOrder.orderPosition = orderPosition;
		em.persist( address );
		em.persist( orderPosition );
		em.persist( product );
		em.persist( customerOrder );

		em.getTransaction().commit();
		em.clear();

		em.getTransaction().begin();

		TypedQuery<CustomerOrder> query = em.createQuery(
				"SELECT o FROM EntityGraphUsingFetchGraphTest$CustomerOrder o left join fetch o.orderPosition pos left join fetch pos.product left join fetch o.shippingAddress", CustomerOrder.class
		);
		final List<CustomerOrder> results = query.getResultList();

		assertTrue( Hibernate.isInitialized( results ) );

		em.getTransaction().commit();
		em.close();
	}

	@Entity
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

		@ManyToOne
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
