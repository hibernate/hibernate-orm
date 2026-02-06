/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

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

import org.hibernate.jpa.AvailableHints;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
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
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-20152")
public class EntityGraphMultipleBagFetchTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product product = new Product();
					product.setName( "test" );
					Tag tag = new Tag();
					tag.setName( "test" );
					Order order = new Order();
					order.setProducts(List.of(product));
					order.setTags(List.of(tag));
					session.persist(order);
				}
		);
	}

	@Test
	void testWithoutEntityGraph(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );

					entityManager
						.createQuery( criteriaQuery(entityManager) )
						.getResultList();

					assertNoJoinFetch(statementInspector);
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-20152",
		reason = "throws MultipleBagFetchException instead of using subsequent selects")
	void testWithEntityGraph(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );

					EntityGraph<Order> entityGraph = entityManager.createEntityGraph( Order.class );
					entityGraph.addAttributeNodes( "products" );
					entityGraph.addAttributeNodes( "tags" );

					entityManager
						.createQuery( criteriaQuery(entityManager) )
						.setHint( AvailableHints.HINT_SPEC_FETCH_GRAPH, entityGraph )
						.getResultList();

					assertNoJoinFetch(statementInspector);
				}
		);
	}

	private CriteriaQuery<Order> criteriaQuery(EntityManager entityManager) {
		CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Order> criteriaQuery = criteriaBuilder.createQuery( Order.class );
		criteriaQuery.from( Order.class );
		return criteriaQuery;
	}

	private void assertNoJoinFetch(SQLStatementInspector statementInspector) {
		statementInspector.assertNumberOfJoins( 0, 0 );
		statementInspector.assertExecutedCount( 3 );
		statementInspector.assertAllSelect();
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

		public void setId(long id) {
			this.id = id;
		}

		public List<Product> getProducts() {
			return this.products;
		}

		public void setProducts(List<Product> products) {
			this.products = products;
		}

		public List<Tag> getTags() {
			return this.tags;
		}

		public void setTags(List<Tag> tags) {
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

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
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

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
