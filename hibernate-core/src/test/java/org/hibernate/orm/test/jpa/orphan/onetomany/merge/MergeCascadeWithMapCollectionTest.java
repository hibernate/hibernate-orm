/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany.merge;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				MergeCascadeWithMapCollectionTest.Parent.class,
				MergeCascadeWithMapCollectionTest.Child.class,
		}
)
@SessionFactory
@JiraKey("HHH-18842")
public class MergeCascadeWithMapCollectionTest {

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
					parent2.addChild( child );

					session.persist( child );
					session.persist( parent2 );
				}
		);
	}

	@AfterEach
	private void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from Parent" ).executeUpdate();
					session.createMutationQuery( "delete from Child" ).executeUpdate();
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
					parent.addChild( child );
					Parent merged = session.merge( parent );
					assertThat( merged.getName() ).isEqualTo( "new name" );
				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, ID_PARENT_WITH_CHILDREN );
					assertThat( parent.getChildren().size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testMergeParentWithChildren2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( ID_PARENT_WITH_CHILDREN, "new name" );
					session.merge( parent );
				}
		);
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, ID_PARENT_WITH_CHILDREN );
					assertThat( parent.getName() ).isEqualTo( "new name" );
					assertThat( parent.getChildren().size() ).isEqualTo( 0 );

					List<Child> children = session.createQuery( "Select c from Child c", Child.class ).list();
					assertThat( children.size() ).isEqualTo( 1 );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.MERGE)
		private Map<String, Child> children;

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

		public Map<String, Child> getChildren() {
			return children;
		}

		public void setChildren(Map<String, Child> children) {
			this.children = children;
		}

		public void addChild(Child child) {
			if ( children == null ) {
				children = new HashMap<>();
			}
			children.put( child.getName(), child );
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

	}

}
