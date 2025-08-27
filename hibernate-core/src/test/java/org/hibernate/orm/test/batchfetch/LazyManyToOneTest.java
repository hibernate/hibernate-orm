/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.Query;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = { LazyManyToOneTest.Child.class, LazyManyToOneTest.Parent.class }
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2"))
@JiraKey("HHH-15449")
public class LazyManyToOneTest {

	public static final String FIELD_VALUE = "a field";
	public static final String FIELD_VALUE_2 = "a second field";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( FIELD_VALUE );
					session.persist( parent );
					session.persist( new Child( parent ) );
					Parent parent2 = new Parent( FIELD_VALUE_2 );
					session.persist( parent2 );
					session.persist( new Child( parent2 ) );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGetResultList(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
				session -> {
					Query<Child> query = session
							.createQuery( "select c from Child as c ", Child.class );
					List<Child> resultList = query.getResultList();
					assertThat( sqlStatementInterceptor.getSqlQueries().size() ).isEqualTo( 1 );


					Parent parent = resultList.get( 0 ).getParent();
					assertThat( parent ).isNotNull();
					Parent parent1 = resultList.get( 1 ).getParent();
					assertThat( parent1 ).isNotNull();
					assertThat( Hibernate.isInitialized( parent ) ).isFalse();
					assertThat( Hibernate.isInitialized( parent1 ) ).isFalse();
					assertThat( sqlStatementInterceptor.getSqlQueries().size() ).isEqualTo( 1 );


					assertThat( parent.getSomeField() ).isEqualTo( FIELD_VALUE );
					assertThat( sqlStatementInterceptor.getSqlQueries().size() ).isEqualTo( 2 );

					assertThat( parent1.getSomeField() ).isEqualTo( FIELD_VALUE_2 );
					// parent2 has been batch loaded
					assertThat( sqlStatementInterceptor.getSqlQueries().size() ).isEqualTo( 2 );
				}
		);
	}

	@Entity(name = "Child")
	@Table(name = "CHILD_TABLE")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id", nullable = false, updatable = false)
		private Parent parent;

		public Child() {
		}

		public Child(Parent parent) {
			this.parent = parent;
		}

		public Parent getParent() {
			return parent;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT_TABLE")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String someField;

		public Parent() {
		}

		public Parent(String someField) {
			this.someField = someField;
		}

		public String getSomeField() {
			return someField;
		}

		public Long getId() {
			return id;
		}

	}


}
