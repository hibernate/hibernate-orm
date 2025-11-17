/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey(value = "HHH-16334")
@DomainModel(
		annotatedClasses = {
				LazyOneToManyPersistAndLoadTest.Parent.class,
				LazyOneToManyPersistAndLoadTest.Child.class
		}
)
@SessionFactory
public class LazyOneToManyPersistAndLoadTest {

	public static final String CHILD_NAME = "Luigi";

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEmptyCollectionPersistLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					session.persist( p );
					List<Child> children = p.getChildren();

					List<Parent> parents = session.createQuery( "from Parent", Parent.class ).getResultList();
					assertThat( parents.size() ).isEqualTo( 1 );

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertThat( children ).isNull();
				}
		);
	}

	@Test
	public void testEmptyCollectionPersistQueryJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, 1l );

					assertFalse( Hibernate.isInitialized( p.getChildren() ) );

					List<Parent> parents = session.createQuery(
							"select p from Parent p left join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();

					assertThat( children ).isEqualTo( parents.get( 0 ).getChildren() );
					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testEmptyCollectionPersistQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, 1l );
					List<Child> children = p.getChildren();
					assertFalse( Hibernate.isInitialized( children ) );

					List<Parent> parents = session.createQuery(
							"select p from Parent p ",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertFalse( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCollectionPersistLoad(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					Child c = new Child( CHILD_NAME );
					p.addChild( c );

					session.persist( c );
					session.persist( p );

					List<Parent> parents = session.createQuery( "from Parent", Parent.class ).getResultList();
					assertThat( parents.size() ).isEqualTo( 1 );

					List<Child> children = parents.get( 0 ).getChildren();
					assertThat( children.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testCollectionPersistQueryJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					Child c = new Child( CHILD_NAME );
					p.addChild( c );
					session.persist( c );
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, 1l );
					assertFalse( Hibernate.isInitialized( p.getChildren() ) );

					List<Child> children = p.getChildren();

					List<Parent> parents = session.createQuery(
							"select p from Parent p join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
				}
		);
	}

	@Test
	public void testCollectionPersistQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( 1l );
					Child c = new Child( CHILD_NAME );
					p.addChild( c );
					session.persist( c );
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, 1l );
					List<Child> children = p.getChildren();

					assertFalse( Hibernate.isInitialized( children ) );

					List<Parent> parents = session.createQuery(
							"select p from Parent p",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					assertFalse( Hibernate.isInitialized( children ) );

					assertThat( children.size() ).isEqualTo( 1 );

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );

					Child child = children.get( 0 );
					assertThat( child.getName() ).isEqualTo( CHILD_NAME );
				}
		);
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(String name) {
			this.name = name;
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

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Child> children;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void addChild(Child c) {
			if ( children == null ) {
				children = new ArrayList<>();
			}
			children.add( c );
			c.setParent( this );
		}

	}

}
