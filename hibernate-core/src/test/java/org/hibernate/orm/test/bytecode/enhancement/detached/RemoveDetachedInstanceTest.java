/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.detached;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				RemoveDetachedInstanceTest.Parent.class,
				RemoveDetachedInstanceTest.Child.class,
				RemoveDetachedInstanceTest.ParentChild.class
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@JiraKey("HHH-18631")
public class RemoveDetachedInstanceTest {
	private static final Long PARENT_ID = 1L;
	private static final Long CHILD_ID = 2L;
	private static final Long PARENT_CHILD_ID = 3L;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent( PARENT_ID, "parent" );
			Child child = new Child( CHILD_ID, "child" );
			ParentChild parentChild = new ParentChild( PARENT_CHILD_ID, parent, child );

			session.persist( parent );
			session.persist( child );
			session.persist( parentChild );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ParentChild" ).executeUpdate();
			session.createMutationQuery( "delete from Child" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	void testRemoveDetachedInstance(SessionFactoryScope scope) {
		ParentChild parentChild = scope.fromTransaction( session -> session.get( ParentChild.class, PARENT_CHILD_ID ) );
		assertThat( parentChild ).isNotNull();

		scope.inTransaction( session -> {
			session.remove( parentChild );
			Parent parent = session.get( Parent.class, PARENT_ID );
			assertThat( parent ).isNotNull();
			List<ParentChild> pc = parent.getChildren();
			assertThat( pc ).isNotNull();
			assertThat( pc.size() ).isEqualTo( 1 );
			assertThat( pc.get( 0 ) ).isSameAs( parentChild );
			Child child = session.get( Child.class, CHILD_ID );
			assertThat( child ).isNotNull();
		} );

		scope.inTransaction( session -> {
			ParentChild pc = session.get( ParentChild.class, PARENT_CHILD_ID );
			assertThat( pc ).isNull();
			Parent parent = session.get( Parent.class, PARENT_ID );
			assertThat( parent ).isNotNull();
			assertThat( parent.getChildren() ).isEmpty();
			Child child = session.get( Child.class, CHILD_ID );
			assertThat( child ).isNotNull();
			assertThat( child.getChildren() ).isEmpty();

		} );
	}

	@Test
	void testRemoveDetachedInstance2(SessionFactoryScope scope) {
		ParentChild parentChild = scope.fromTransaction( session -> session.get( ParentChild.class, PARENT_CHILD_ID ) );
		assertThat( parentChild ).isNotNull();

		scope.inTransaction( session -> {
			session.remove( parentChild );
			session.remove( parentChild.getChild() );
			Parent parent = session.get( Parent.class, PARENT_ID );
			assertThat( parent ).isNotNull();
			List<ParentChild> pc = parent.getChildren();
			assertThat( pc ).isNotNull();
			assertThat( pc.size() ).isEqualTo( 1 );
			assertThat( pc.get( 0 ) ).isSameAs( parentChild );
			Child child = session.get( Child.class, CHILD_ID );
			assertThat( child ).isNull();
		} );

		scope.inTransaction( session -> {
			ParentChild pc = session.get( ParentChild.class, PARENT_CHILD_ID );
			assertThat( pc ).isNull();
			Parent parent = session.get( Parent.class, PARENT_ID );
			assertThat( parent ).isNotNull();
			assertThat( parent.getChildren() ).isEmpty();
			Child child = session.get( Child.class, CHILD_ID );
			assertThat( child ).isNull();
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
		List<ParentChild> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<ParentChild> getChildren() {
			return children;
		}

		public void setChildren(List<ParentChild> children) {
			this.children = children;
		}
	}

	@Entity(name = "ParentChild")
	public static class ParentChild {
		@Id
		private Long id;

		@ManyToOne
		private Parent parent;

		@ManyToOne
		private Child child;

		public ParentChild() {
		}

		public ParentChild(Long id, Parent parent, Child child) {
			this.id = id;
			this.parent = parent;
			this.child = child;
			parent.getChildren().add( this );
			child.getChildren().add( this );
		}

		public Long getId() {
			return id;
		}

		public Parent getParent() {
			return parent;
		}

		public Child getChild() {
			return child;
		}

	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "child")
		@OrderColumn
		List<ParentChild> children = new ArrayList<>();

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<ParentChild> getChildren() {
			return children;
		}

		public void setChildren(List<ParentChild> children) {
			this.children = children;
		}
	}
}
