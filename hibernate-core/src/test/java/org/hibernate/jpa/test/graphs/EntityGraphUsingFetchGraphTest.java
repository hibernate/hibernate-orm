/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs;

import javax.persistence.AttributeNode;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
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
import javax.persistence.metamodel.Attribute;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
	public void fetchAttributeNodeByStringFromSubgraph() {
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

		assertEntityGraph( entityGraph );
		assertTrue( Hibernate.isInitialized( results ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13233")
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void fetchAttributeNodeByAttributeFromSubgraph() {
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
		EntityTypeDescriptor<CustomerOrder> customerOrderEntityType =
			entityManagerFactory().getMetamodel().entity( CustomerOrder.class );
		entityGraph.addAttributeNodes(
			(Attribute) customerOrderEntityType.getAttribute( "shippingAddress" ),
			(Attribute) customerOrderEntityType.getAttribute( "orderDate" )
		);
		entityGraph.addAttributeNodes( (Attribute) customerOrderEntityType.getAttribute( "shippingAddress" ) );

		final Subgraph<OrderPosition> orderProductsSubgraph =
			entityGraph.addSubgraph( (Attribute) customerOrderEntityType.getAttribute( "orderPosition" ) );
		EntityTypeDescriptor<OrderPosition> positionEntityType =
			entityManagerFactory().getMetamodel().entity( OrderPosition.class );
		orderProductsSubgraph.addAttributeNodes( (Attribute) positionEntityType.getAttribute( "amount" ) );
		orderProductsSubgraph.addAttributeNodes( (Attribute) positionEntityType.getAttribute( "product" ) );

		final Subgraph<Product> productSubgraph =
			orderProductsSubgraph.addSubgraph( (Attribute) positionEntityType.getAttribute( "product" ) );
		EntityTypeDescriptor<Product> productEntityType = entityManagerFactory().getMetamodel().entity( Product.class );
		productSubgraph.addAttributeNodes( (Attribute) productEntityType.getAttribute( "productName" ) );

		TypedQuery<CustomerOrder> query = em.createQuery(
			"SELECT o FROM EntityGraphUsingFetchGraphTest$CustomerOrder o", CustomerOrder.class
		);
		query.setHint( "javax.persistence.loadgraph", entityGraph );
		final List<CustomerOrder> results = query.getResultList();

		assertEntityGraph( entityGraph );
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


	/**
	 * Verify that entityGraph has expected state
	 *
	 * customerOrder - shippingAddress
	 *               - orderDate
	 *               - orderPosition - amount
	 *                               - product - productName
	 *
	 * @param entityGraph entityGraph
	 */
	private void assertEntityGraph(EntityGraph<CustomerOrder> entityGraph) {
		assertEquals(3, entityGraph.getAttributeNodes().size());
		for ( AttributeNode<?> entityGraphAttributeNode : entityGraph.getAttributeNodes() ) {
			if ( "orderPosition".equals( entityGraphAttributeNode.getAttributeName() ) ) {
				Collection<Subgraph> orderPositionGraph = entityGraphAttributeNode.getSubgraphs().values();
				assertEquals( 1, orderPositionGraph.size() );
				List<AttributeNode<?>> orderPositionAttributes = orderPositionGraph.iterator().next().getAttributeNodes();
				assertEquals( 2, orderPositionAttributes.size() );
				for ( AttributeNode<?> orderPositionAttributeNode : orderPositionAttributes ) {
					if ( "product".equals( orderPositionAttributeNode.getAttributeName() ) ) {
						assertEquals( 1, orderPositionAttributeNode.getSubgraphs().size() );
					} else {
						assertTrue( orderPositionAttributeNode.getSubgraphs().isEmpty() );
					}
				}
			} else {
				assertTrue( entityGraphAttributeNode.getSubgraphs().isEmpty() );
			}
		}
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
