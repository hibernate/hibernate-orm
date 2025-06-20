/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.graphs.queryhint;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Subgraph;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOAD_GRAPH;

@JiraKey("HHH-14349")
@DomainModel(
		annotatedClasses = {
				EmbeddableQueryHintEntityGraphTest.Parent.class,
				EmbeddableQueryHintEntityGraphTest.Child.class,
				EmbeddableQueryHintEntityGraphTest.AnotherChild.class
		}
)
@SessionFactory
public class EmbeddableQueryHintEntityGraphTest {

	private final static Long PARENT_ID = 1L;

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( PARENT_ID, "parent" );
					session.persist( parent );

					Child child1 = new Child( 1L, "child1", parent );
					session.persist( child1 );

					Child child2 = new Child( 2L, "child2", parent );
					session.persist( child2 );

					AnotherChild anotherChild = new AnotherChild( 1L, "child3" );
					session.persist( anotherChild );

					AnEmbeddable embedded = new AnEmbeddable( new AnotherEmbeddable( anotherChild ), child1 );
					embedded.addChild( child2 );
					parent.setEmbedded( embedded );
				}
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testFetchingFromManagedEntityEmbeddedBasicFieldLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = session.find( Parent.class, PARENT_ID );
					assertIsInitialized( parent );
					assertIsNotInitialized( parent.getEmbedded().getChild() );

					final EntityGraph<Parent> entityGraph = session.createEntityGraph( Parent.class );
					Subgraph<Object> subGraph = entityGraph.addSubgraph( "embedded" );
					subGraph.addAttributeNode( "child" );

					Parent p = selectParent( entityGraph, session );
					assertIsInitialized( p );
					assertIsInitialized( p.getEmbedded().getChild() );
				}
		);
	}

	@Test
	public void testFetchingFromManagedEntityEmbeddedCollectionFieldLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = session.find( Parent.class, PARENT_ID );
					assertIsInitialized( parent );
					assertIsNotInitialized( parent.getEmbedded().getChildren() );

					final EntityGraph<Parent> entityGraph = session.createEntityGraph( Parent.class );
					Subgraph<Object> subGraph = entityGraph.addSubgraph( "embedded" );
					subGraph.addAttributeNode( "children" );

					Parent p = selectParent( entityGraph, session );
					assertIsInitialized( p );
					assertIsInitialized( p.getEmbedded().getChildren() );
				}
		);
	}

	@Test
	public void testFetchingFromManagedEntityNestedEmbeddedBasicFieldLoadGraph(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Parent parent = session.get( Parent.class, PARENT_ID );
					assertIsInitialized( parent );
					assertIsNotInitialized( parent.getEmbedded().getNestedEmbedded().getAnotherChild() );

					final EntityGraph<Parent> entityGraph = session.createEntityGraph( Parent.class );
					Subgraph<Object> subGraph = entityGraph.addSubgraph( "embedded" );
					Subgraph<Object> nestedSubgraph = subGraph.addSubgraph( "nestedEmbedded" );
					nestedSubgraph.addAttributeNode( "anotherChild" );

					Parent p = selectParent( entityGraph, session );
					assertIsInitialized( p );
					assertIsInitialized( p.getEmbedded().getNestedEmbedded().getAnotherChild() );
				}
		);
	}

	static Parent selectParent(EntityGraph<Parent> entityGraph, Session session) {
		final Query<Parent> query = session.createQuery( "select p from Parent p", Parent.class );
		return query.setHint( HINT_SPEC_LOAD_GRAPH, entityGraph ).uniqueResult();
	}

	static void assertIsInitialized(Object entity) {
		assertThat( Hibernate.isInitialized( entity ) ).isTrue();
	}

	static void assertIsNotInitialized(Object entity) {
		assertThat( Hibernate.isInitialized( entity ) ).isFalse();
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@Embedded
		private AnEmbeddable embedded;

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

		public AnEmbeddable getEmbedded() {
			return embedded;
		}

		public void setEmbedded(AnEmbeddable embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class AnEmbeddable {

		@Embedded
		private AnotherEmbeddable nestedEmbedded;

		@ManyToOne(fetch = FetchType.LAZY)
		private Child child;

		@OneToMany(fetch = FetchType.LAZY)
		private List<Child> children = new ArrayList<>();

		public AnEmbeddable() {
		}

		public AnEmbeddable(AnotherEmbeddable nestedEmbeddedObject, Child child) {
			this.nestedEmbedded = nestedEmbeddedObject;
			this.child = child;
		}

		public AnotherEmbeddable getNestedEmbedded() {
			return nestedEmbedded;
		}

		public Child getChild() {
			return child;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			this.children.add( child );
		}
	}

	@Embeddable
	public static class AnotherEmbeddable {
		@ManyToOne(fetch = FetchType.LAZY)
		private AnotherChild anotherChild;

		public AnotherEmbeddable() {
		}

		public AnotherEmbeddable(AnotherChild anotherChild) {
			this.anotherChild = anotherChild;
		}

		public AnotherChild getAnotherChild() {
			return anotherChild;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Child(Long id, String name, Parent parent) {
			this.id = id;
			this.name = name;
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

	@Entity(name = "AnotherChild")
	public static class AnotherChild {
		@Id
		private Long id;

		private String name;

		public AnotherChild() {
		}

		public AnotherChild(Long id, String name) {
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
