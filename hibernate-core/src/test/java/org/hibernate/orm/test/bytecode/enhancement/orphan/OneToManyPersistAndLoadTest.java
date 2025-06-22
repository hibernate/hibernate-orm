/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.orphan;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JiraKey("HHH-16334")
@DomainModel(
		annotatedClasses = {
				OneToManyPersistAndLoadTest.Parent.class,
				OneToManyPersistAndLoadTest.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({
		EnhancerTestContext.class, // supports laziness and dirty-checking
		DefaultEnhancementContext.class
})
public class OneToManyPersistAndLoadTest {

	public static final String CHILD_NAME = "Luigi";
	public static final String CHILD_NAME_2 = "Fab1";

	public static final String CHILD_NAME_3 = "Fab2";
	public static final String CHILD_NAME_4 = "Fab3";
	public static final String CHILD_NAME_5 = "Fab4";

	public static final String CHILD_NAME_6 = "Fab5";
	public static final String CHILD_NAME_7 = "Fab6";
	public static final String CHILD_NAME_8 = "Fab7";
	public static final String CHILD_NAME_9 = "Fab8";

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
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

					List<Parent> parents = session.createQuery(
							"select p from Parent p left join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					Set<Child> children = p.getChildren();
					Set<Child> children2 = p.getChildren2();
					Set<Child> children3 = p.getChildren3();

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertThat( parents.get( 0 ).getChildren2() ).isEqualTo( children2 );
					assertThat( parents.get( 0 ).getChildren3() ).isEqualTo( children3 );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
					assertThat( children2.size() ).isEqualTo( 0 );
					assertThat( children3.size() ).isEqualTo( 0 );
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

					List<Parent> parents = session.createQuery(
							"select p from Parent p ",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					Set<Child> children = p.getChildren();
					Set<Child> children2 = p.getChildren2();
					Set<Child> children3 = p.getChildren3();

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertThat( parents.get( 0 ).getChildren2() ).isEqualTo( children2 );
					assertThat( parents.get( 0 ).getChildren3() ).isEqualTo( children3 );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 0 );
					assertThat( children2.size() ).isEqualTo( 0 );
					assertThat( children3.size() ).isEqualTo( 0 );
				}
		);
	}

	@Test
	public void testCollectionPersistQueryJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					populateParentWithChildren( session );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, 1l );

					List<Parent> parents = session.createQuery(
							"select distinct p from Parent p join fetch p.children",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					Set<Child> children = p.getChildren();
					Set<Child> children2 = p.getChildren2();
					Set<Child> children3 = p.getChildren3();

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertThat( parents.get( 0 ).getChildren2() ).isEqualTo( children2 );
					assertThat( parents.get( 0 ).getChildren3() ).isEqualTo( children3 );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 2 );
					assertThat( children2.size() ).isEqualTo( 3 );
					assertThat( children3.size() ).isEqualTo( 4 );
				}
		);
	}

	@Test
	public void testCollectionPersistQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					populateParentWithChildren( session );
				}
		);

		scope.inTransaction(
				session -> {
					Parent p = session.get( Parent.class, 1l );
					List<Parent> parents = session.createQuery(
							"select p from Parent p",
							Parent.class
					).getResultList();

					assertThat( parents.size() ).isEqualTo( 1 );
					assertThat( parents.get( 0 ) ).isEqualTo( p );

					Set<Child> children = p.getChildren();
					Set<Child> children2 = p.getChildren2();
					Set<Child> children3 = p.getChildren3();

					assertThat( parents.get( 0 ).getChildren() ).isEqualTo( children );
					assertThat( parents.get( 0 ).getChildren2() ).isEqualTo( children2 );
					assertThat( parents.get( 0 ).getChildren3() ).isEqualTo( children3 );

					assertTrue( Hibernate.isInitialized( children ) );
					assertThat( children.size() ).isEqualTo( 2 );
					assertThat( children2.size() ).isEqualTo( 3 );
					assertThat( children3.size() ).isEqualTo( 4 );
				}
		);
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

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

	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		private Set<Child> children;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
		@JoinTable(name = "Parent_child2")
		private Set<Child> children2;

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinTable(name = "Parent_child3")
		private Set<Child> children3;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public Set<Child> getChildren2() {
			return children2;
		}

		public Set<Child> getChildren3() {
			return children3;
		}

		public void addChild(Child c) {
			if ( children == null ) {
				children = new HashSet<>();
			}
			children.add( c );
		}

		public void addChild2(Child c) {
			if ( children2 == null ) {
				children2 = new HashSet<>();
			}
			children2.add( c );
		}

		public void addChild3(Child c) {
			if ( children3 == null ) {
				children3 = new HashSet<>();
			}
			children3.add( c );
		}

	}

	private static void populateParentWithChildren(SessionImplementor session) {
		Parent p = new Parent( 1l );
		Child c = new Child( CHILD_NAME );
		p.addChild( c );
		Child c2 = new Child( CHILD_NAME_2 );
		p.addChild( c2 );

		Child c3 = new Child( CHILD_NAME_3 );
		p.addChild2( c3 );
		Child c4 = new Child( CHILD_NAME_4 );
		p.addChild2( c4 );
		Child c5 = new Child( CHILD_NAME_5 );
		p.addChild2( c5 );

		Child c6 = new Child( CHILD_NAME_6 );
		p.addChild3( c6 );
		Child c7 = new Child( CHILD_NAME_7 );
		p.addChild3( c7 );
		Child c8 = new Child( CHILD_NAME_8 );
		p.addChild3( c8 );
		Child c9 = new Child( CHILD_NAME_9 );
		p.addChild3( c9 );

		session.persist( c );
		session.persist( c2 );
		session.persist( c3 );
		session.persist( c4 );
		session.persist( c5 );
		session.persist( c6 );
		session.persist( c7 );
		session.persist( c8 );
		session.persist( c9 );
		session.persist( p );
	}

}
