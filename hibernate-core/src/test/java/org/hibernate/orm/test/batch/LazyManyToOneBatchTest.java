/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = {
				LazyManyToOneBatchTest.Child.class,
				LazyManyToOneBatchTest.Parent.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2")
)
@JiraKey(value = "HHH-15346")
public class LazyManyToOneBatchTest {
	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 1L, "a" );
					session.persist( parent );

					Parent parent2 = new Parent( 2L, "b" );
					session.persist( parent2 );

					Parent parent3 = new Parent( 3L, "c" );
					session.persist( parent3 );

					Child child1 = new Child( 4L, parent );
					session.persist( child1 );

					Child child2 = new Child( 5L, parent );
					session.persist( child2 );

					Child child3 = new Child( 6L, parent2 );
					session.persist( child3 );

					Child child4 = new Child( 7L, parent3 );
					session.persist( child4 );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					List<Child> children = session
							.createQuery( "select c from Child as c ", Child.class )
							.getResultList();
					statementInspector.assertExecutedCount( 1 );
					assertEquals( 4, children.size() );

					Child child1 = children.get( 0 );
					assertNotNull( child1.getParent() );
					assertThat( child1.getParent().getName(), is( "a" ) );

					statementInspector.assertExecutedCount( 2 );

					Child child2 = children.get( 1 );
					assertNotNull( child2.getParent() );
					assertThat( child2.getParent().getName(), is( "a" ) );

					statementInspector.assertExecutedCount( 2 );

					Child child3 = children.get( 2 );
					assertNotNull( child3.getParent() );
					assertThat( child3.getParent().getName(), is( "b" ) );
					statementInspector.assertExecutedCount( 2 );

					Child child4 = children.get( 3 );
					assertNotNull( child4.getParent() );
					assertThat( child4.getParent().getName(), is( "c" ) );
					statementInspector.assertExecutedCount( 3 );
				}
		);
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		@ManyToOne(optional = false, fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id", nullable = false, updatable = false)
		private Parent parent;

		public Child() {
		}

		public Child(Long id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Parent getParent() {
			return parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		public Parent() {
		}

		public Parent(Long id, String name) {
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
