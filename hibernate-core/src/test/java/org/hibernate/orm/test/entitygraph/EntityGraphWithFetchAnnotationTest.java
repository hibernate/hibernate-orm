/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.entitygraph;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Vlad Mihalcea
 */
@DomainModel(
		annotatedClasses = {
				EntityGraphWithFetchAnnotationTest.Order.class,
				EntityGraphWithFetchAnnotationTest.Product.class,
				EntityGraphWithFetchAnnotationTest.Tag.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class EntityGraphWithFetchAnnotationTest {

	@Test
	@JiraKey(value = "HHH-10485")
	void testWithoutEntityGraph(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Order> criteriaQuery = criteriaBuilder
							.createQuery( Order.class );
					criteriaQuery.from( Order.class );

					entityManager
							.createQuery( criteriaQuery )
							.setFirstResult( 10 )
							.setMaxResults( 20 )
							.getResultList();

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "left join", 0 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10485")
	void testWithEntityGraph(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction(
				session -> {
					EntityManager entityManager = session.unwrap( EntityManager.class );
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Order> criteriaQuery = criteriaBuilder
							.createQuery( Order.class );
					criteriaQuery.from( Order.class );

					EntityGraph<Order> entityGraph = entityManager.createEntityGraph( Order.class );
					entityGraph.addAttributeNodes( "products" );

					entityManager
							.createQuery( criteriaQuery )
							.setFirstResult( 10 )
							.setMaxResults( 20 )
							.setHint( GraphSemantic.FETCH.getJpaHintName(), entityGraph )
							.getResultList();

					statementInspector.assertExecutedCount( 1 );
					String sql = statementInspector.getSqlQueries().get( 0 );
					assertThat( sql, containsString( "left join" ) );
				}
		);
	}

	@Entity(name = "Order")
	@Table(name = "orders")
	public static class Order {

		@Id
		@GeneratedValue
		private long id;

		@OneToMany
		@Fetch(FetchMode.SELECT)
		private List<Product> products;

		@OneToMany
		@Fetch(FetchMode.SELECT)
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

	@Entity(name = "Product")
	@Table(name = "products")
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

	@Entity(name = "Tag")
	@Table(name = "tags")
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
