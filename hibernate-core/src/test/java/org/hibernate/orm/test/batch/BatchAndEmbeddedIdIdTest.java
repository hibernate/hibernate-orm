/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false" )
		}
)
@DomainModel(
		annotatedClasses = {
				BatchAndEmbeddedIdIdTest.Child.class,
				BatchAndEmbeddedIdIdTest.Parent.class
		}
)
@SessionFactory(
		useCollectingStatementInspector = true
)
@JiraKey(value = "HHH-15921")
public class BatchAndEmbeddedIdIdTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 10; i++ ) {
						Parent parent = new Parent( (long) i );
						Child child = new Child( (long) ( i + 1 ), parent );
						Child child2 = new Child( (long) ( i + 2 ), parent );
						Child child3 = new Child( (long) ( i + 3 ), parent );
						Child child4 = new Child( (long) ( i + 4 ), parent );
						Child child5 = new Child( (long) ( i + 5 ), parent );
						Child child6 = new Child( (long) ( i + 6 ), parent );
						Child child7 = new Child( (long) ( i + 7 ), parent );
						Child child8 = new Child( (long) ( i + 8 ), parent );
						Child child9 = new Child( (long) ( i + 9 ), parent );
						Child child10 = new Child( (long) ( i + 10 ), parent );
						Child child11 = new Child( (long) ( i + 11 ), parent );

						session.persist( parent );
					}
				}
		);
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.find( Parent.class, 1L );
					assertThat( parent.getChildren().size() ).isEqualTo( 11 );
				}
		);
	}

	@Test
	public void testSelectChild(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		scope.inTransaction( (session) -> {
			statementInspector.clear();
			List<Child> children = session.createQuery( "select c from Child c", Child.class ).getResultList();
			statementInspector.assertExecutedCount( 3 );
			if ( scope.getSessionFactory().getJdbcServices().getDialect().useArrayForMultiValuedParameters() ) {
				assertThat( statementInspector.getSqlQueries().get( 1 ) ).containsOnlyOnce( "?" );
				assertThat( statementInspector.getSqlQueries().get( 2 ) ).containsOnlyOnce( "?" );
			}
			else {
				assertThat( statementInspector.getSqlQueries().get( 1 ) ).containsOnlyOnce( "in (?,?,?,?,?)" );
				assertThat( statementInspector.getSqlQueries().get( 2 ) ).containsOnlyOnce( "in (?,?,?,?,?)" );
			}

			statementInspector.clear();
			for ( Child c : children ) {
				c.getParent().getName();
			}
			statementInspector.assertExecutedCount( 0 );
		} );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.getReference( Parent.class, 1l );
					Parent parent1 = session.getReference( Parent.class, 2l );
					Parent parent2 = session.getReference( Parent.class, 3l );
					assertFalse( Hibernate.isInitialized( parent ) );
					assertFalse( Hibernate.isInitialized( parent1 ) );
					assertFalse( Hibernate.isInitialized( parent2 ) );

					parent.getName();

					assertTrue( Hibernate.isInitialized( parent ) );
					assertTrue( Hibernate.isInitialized( parent1 ) );
					assertTrue( Hibernate.isInitialized( parent2 ) );

				}
		);
	}

	@Entity(name = "Child")
	@Table(name = "child_tablle")
	public static class Child {
		@EmbeddedId
		private EmbeddableId id;

		private String name;


		public Child() {
		}

		public Child(Long id, Parent parent) {
			this.id = new EmbeddableId( id, parent );
			this.name = String.valueOf( id );
			parent.addChild( this );
		}

		public EmbeddableId getId() {
			return id;
		}

		public String getName() {
			return name;
		}


		public Parent getParent() {
			return id.getParent();
		}
	}

	@Embeddable
	public static class EmbeddableId implements Serializable {
		private long id;

		@ManyToOne
		@JoinColumn(name = "parent_id")
		private Parent parent;


		public EmbeddableId() {
		}

		public EmbeddableId(Long id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}

		public long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "parents")
	@BatchSize(size = 5)
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "id.parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
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

		public void addChild(Child child) {
			children.add( child );
		}
	}
}
