/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.assertj.core.api.Assertions;

@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false" )
		}
)
@DomainModel(
		annotatedClasses = {
				BatchAndClassIdCollectionTest.Child.class,
				BatchAndClassIdCollectionTest.Parent.class
		}
)
@SessionFactory(
		useCollectingStatementInspector = true
)
@JiraKey("HHH-17202")
public class BatchAndClassIdCollectionTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for (long i = 1L; i < 11; i++) {
						Parent parent = new Parent( i );
						Child child = new Child( i * 100L + 1L, parent );
						Child child2 = new Child( i * 100L + 2L, parent );
						Child child3 = new Child( i * 100L + 3L, parent );
						Child child4 = new Child( i * 100L + 4L, parent );
						Child child5 = new Child( i * 100L + 5L, parent );
						Child child6 = new Child( i * 100L + 6L, parent );
						Child child7 = new Child( i * 100L + 7L, parent );
						Child child8 = new Child( i * 100L + 8L, parent );
						Child child9 = new Child( i * 100L + 9L, parent );
						Child child10 = new Child( i * 100L + 10L, parent );
						Child child11 = new Child( i * 100L + 11L, parent );
						session.persist( parent );
					}
				}
		);
	}

	@Test
	public void testBatchInitializeChildCollection(SessionFactoryScope scope){
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					final List<Parent> list = session.createSelectionQuery( "from Parent", Parent.class )
							.getResultList();
					list.get( 0 ).getChildren().size();
					statementInspector.assertExecutedCount( 2 );
					Assertions.assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( "?" );
					if ( scope.getSessionFactory().getJdbcServices().getDialect().useArrayForMultiValuedParameters() ) {
						Assertions.assertThat( statementInspector.getSqlQueries().get( 1 ) ).containsOnlyOnce( "?" );
					}
					else {
						Assertions.assertThat( statementInspector.getSqlQueries().get( 1 ) ).containsOnlyOnce( "in (?,?,?,?,?)" );
					}
				}
		);
	}

	@Entity(name = "Child")
	@Table(name = "child_tablle")
	@IdClass(Child.IdClass.class)
	public static class Child {
		@Id
		private Long id;

		private String name;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private Parent parent;

		public Child() {
		}

		public Child(Long id, Parent parent) {
			this.id = id;
			this.name = String.valueOf( id );
			this.parent = parent;
			parent.addChild( this );
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

		public static class IdClass implements Serializable {
			private long id;

			public IdClass() {
			}

			public IdClass(long id) {
				this.id = id;
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}

				Parent.IdClass idClass = (Parent.IdClass) o;

				return id == idClass.id;
			}

			@Override
			public int hashCode() {
				return (int) ( id ^ ( id >>> 32 ) );
			}

		}
	}

	@Entity(name = "Parent")
	@Table(name = "parents")
	@IdClass(Parent.IdClass.class)
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@BatchSize(size = 5)
		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
		public Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
			this.name = String.valueOf( id );
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void addChild(Child child){
			children.add( child );
		}

		public static class IdClass implements Serializable {
			private long id;

			public IdClass() {
			}

			public IdClass(long id) {
				this.id = id;
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}

				IdClass idClass = (IdClass) o;

				return id == idClass.id;
			}

			@Override
			public int hashCode() {
				return (int) ( id ^ ( id >>> 32 ) );
			}
		}
	}
}
