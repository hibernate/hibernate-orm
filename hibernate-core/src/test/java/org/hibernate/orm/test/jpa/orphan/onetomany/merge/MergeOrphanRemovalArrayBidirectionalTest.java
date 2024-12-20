/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany.merge;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				MergeOrphanRemovalArrayBidirectionalTest.Parent.class,
				MergeOrphanRemovalArrayBidirectionalTest.Child.class,
		}
)
@SessionFactory
@JiraKey("HHH-18842")
public class MergeOrphanRemovalArrayBidirectionalTest {

	private static final Long ID_PARENT_WITHOUT_CHILDREN = 1L;
	private static final Long ID_PARENT_WITH_CHILDREN = 2L;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( ID_PARENT_WITHOUT_CHILDREN, "old name" );
					session.persist( parent );

					Parent parent2 = new Parent( ID_PARENT_WITH_CHILDREN, "old name" );
					Child child = new Child( 2l, "Child" );
					Child[] children = new Child[1];
					children[0] = child;
					parent2.setChildren( children );

					session.persist( child );
					session.persist( parent2 );
				}
		);
	}

	@AfterEach
	private void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Child" ).executeUpdate();
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMergeParentWihoutChildren(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( ID_PARENT_WITHOUT_CHILDREN, "new name" );
					Parent merged = session.merge( parent );
					assertThat( merged.getName() ).isEqualTo( "new name" );

				}
		);
	}

	@Test
	public void testMergeParentWithChildren(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( ID_PARENT_WITH_CHILDREN, "new name" );
					Child child = new Child( 2l, "Child" );
					Child[] children = new Child[1];
					children[0] = child;
					parent.setChildren( children );
					Parent merged = session.merge( parent );
					assertThat( merged.getName() ).isEqualTo( "new name" );

				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, ID_PARENT_WITH_CHILDREN );
					assertThat( parent.getChildren().length ).isEqualTo( 1 );

				}
		);
	}

	@Test
	public void testMergeParentWithChildren2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( ID_PARENT_WITH_CHILDREN, "new name" );
					Parent merged = session.merge( parent );

				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, ID_PARENT_WITH_CHILDREN );
					assertThat( parent.getName() ).isEqualTo( "new name" );
					assertThat( parent.getChildren().length ).isEqualTo( 0 );

					List<Child> children = session.createQuery( "Select c from Child c", Child.class ).list();
					assertThat( children.size() ).isEqualTo( 0 );
				}
		);

	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToMany(orphanRemoval = true, mappedBy = "parent")
		private Child[] children;

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

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Child[] getChildren() {
			return children;
		}

		public void setChildren(Child[] children) {
			this.children = children;
			for ( int i = 0; i < children.length; i++ ) {
				children[i].setParent( this );
			}
		}

	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		private Long id;

		private String name;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@ManyToOne
		@JoinColumn
		private Parent parent;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

}
