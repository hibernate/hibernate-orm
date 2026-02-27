/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.Hibernate;
import org.hibernate.jpa.AvailableHints;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				EntityGraphMultipleBagFetchTest.Order.class,
				EntityGraphMultipleBagFetchTest.Product.class,
				EntityGraphMultipleBagFetchTest.Tag.class
		}
)
@SessionFactory( useCollectingStatementInspector = true )
@JiraKey( value = "HHH-20152" )
public class EntityGraphMultipleBagFetchTest {

	@BeforeAll
	public void setUp( SessionFactoryScope scope ) {
		scope.inTransaction(
				session -> {
					Product product = new Product();
					product.setName( "test" );
					Tag tag = new Tag();
					tag.setName( "test" );
					Order order = new Order();
					order.setProducts( List.of( product ));
					order.setTags( List.of( tag ));
					session.persist( order );
				}
		);
	}

	@Test
	void testWithoutEntityGraph( SessionFactoryScope scope ) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );

					List<Order> orders = entityManager
						.createQuery( criteriaQuery( entityManager ) )
						.getResultList();

					// Expect three SELECT statements
					statementInspector.assertExecutedCount( 3 );
					statementInspector.assertAllSelect();
					// First one fetches parent entity, subsequent selects for bags
					for ( int i = 0; i < 3; i++ ) {
						statementInspector.assertNumberOfJoins( i, 0 );
					}

					assertInitialized( orders.get( 0 ));
				}
		);
	}

	@Test
	void testWithEntityGraph( SessionFactoryScope scope ) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );

					EntityGraph<Order> entityGraph = entityManager.createEntityGraph( Order.class );
					entityGraph.addAttributeNodes( "products" );
					entityGraph.addAttributeNodes( "tags" );

					List<Order> orders = entityManager
						.createQuery( criteriaQuery( entityManager ) )
						.setHint( AvailableHints.HINT_SPEC_FETCH_GRAPH, entityGraph )
						.getResultList();

					// Expect two SELECT statements
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertAllSelect();
					// First one join fetches first bag
					statementInspector.assertNumberOfJoins( 0, 1 );
					// Subsequent select for second bag
					statementInspector.assertNumberOfJoins( 1, 0 );

					assertInitialized( orders.get( 0 ));
				}
		);
	}

	@Test
	void testWithJoinFetch( SessionFactoryScope scope ) {
		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );

					// Cannot explicitly join fetch multiple bags
					Throwable e = assertThrows( IllegalArgumentException.class, () -> entityManager
						.createQuery( "from EntityGraphMultipleBagFetchTest$Order o join fetch o.products join fetch o.tags" )
						.getResultList() );
					assertEquals( MultipleBagFetchException.class, e.getCause().getClass() );
				}
		);
	}

	private CriteriaQuery<Order> criteriaQuery( EntityManager entityManager ) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Order> criteriaQuery = criteriaBuilder.createQuery( Order.class );
		criteriaQuery.from( Order.class );
		return criteriaQuery;
	}

	private void assertInitialized( Order order ) {
		assertTrue( Hibernate.isInitialized( order ) );
		assertTrue( Hibernate.isInitialized( order.products ) );
		assertTrue( Hibernate.isInitialized( order.tags ) );
	}

	@Entity
	public static class Order {

		@Id
		@GeneratedValue
		private long id;

		@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL )
		@JoinColumn( name = "order_id" )
		private List<Product> products;

		@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL )
		@JoinColumn( name = "order_id" )
		private List<Tag> tags;

		public long getId() {
			return this.id;
		}

		public void setId( long id ) {
			this.id = id;
		}

		public List<Product> getProducts() {
			return this.products;
		}

		public void setProducts( List<Product> products ) {
			this.products = products;
		}

		public List<Tag> getTags() {
			return this.tags;
		}

		public void setTags( List<Tag> tags ) {
			this.tags = tags;
		}
	}

	@Entity
	public static class Product {

		@Id
		@GeneratedValue
		private long id;

		private String name;

		public long getId() {
			return this.id;
		}

		public void setId( long id ) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName( String name ) {
			this.name = name;
		}
	}

	@Entity
	public static class Tag {

		@Id
		@GeneratedValue
		private long id;

		private String name;

		public long getId() {
			return this.id;
		}

		public void setId( long id ) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName( String name ) {
			this.name = name;
		}
	}
}
