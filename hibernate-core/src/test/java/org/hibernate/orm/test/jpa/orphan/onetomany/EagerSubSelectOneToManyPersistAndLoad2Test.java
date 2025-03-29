/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey(value = "HHH-16334")
@DomainModel(
		annotatedClasses = {
				EagerSubSelectOneToManyPersistAndLoad2Test.Parent.class,
				EagerSubSelectOneToManyPersistAndLoad2Test.Child.class
		}
)
@SessionFactory
public class EagerSubSelectOneToManyPersistAndLoad2Test {

	public static final String CHILD_NAME = "Luigi";


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Child" ).executeUpdate();
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
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
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p left join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertTrue( Hibernate.isInitialized( children ) );

					assertThat( children ).isEqualTo( parents.get( 0 ).getChildren() );
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
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p ",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
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
					Parent p = session.getReference( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select p from Parent p join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 1 );
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
					Parent p = session.getReference( Parent.class, 1l );
					List<Parent> parents = session.createQuery(
							"select p from Parent p",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					List<Child> children = p.getChildren();
					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 1 );


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

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@Fetch(FetchMode.SUBSELECT)
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
