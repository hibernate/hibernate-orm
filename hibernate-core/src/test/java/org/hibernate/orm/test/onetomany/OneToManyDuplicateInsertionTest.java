/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(
		annotatedClasses = {
				OneToManyDuplicateInsertionTest.Parent.class,
				OneToManyDuplicateInsertionTest.Child.class,
				OneToManyDuplicateInsertionTest.ParentCascade.class,
				OneToManyDuplicateInsertionTest.ChildCascade.class
		}
)
public class OneToManyDuplicateInsertionTest {

	private int parentId;

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-6776")
	public void testDuplicateInsertion(EntityManagerFactoryScope scope) {
		// persist parent entity in a transaction

		scope.inTransaction( em -> {
			Parent parent = new Parent();
			em.persist( parent );
			parentId = parent.getId();
		} );

		// relate and persist child entity in another transaction

		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, parentId );
			Child child = new Child();
			child.setParent( parent );
			parent.getChildren().add( child );
			em.persist( child );

			assertEquals( 1, parent.getChildren().size() );
		} );

		// get the parent again

		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, parentId );

			assertEquals( 1, parent.getChildren().size() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-7404")
	public void testDuplicateInsertionWithCascadeAndMerge(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			ParentCascade p = new ParentCascade();
			// merge with 0 children
			p = em.merge( p );
			parentId = p.getId();
		} );

		scope.inTransaction( em -> {
			ParentCascade p = em.find( ParentCascade.class, parentId );
			final ChildCascade child = new ChildCascade();
			child.setParent( p );
			p.getChildren().add( child );
			em.merge( p );
		} );

		scope.inTransaction( em -> {
			// again, load the Parent by id
			ParentCascade p = em.find( ParentCascade.class, parentId );

			// check that we have only 1 element in the list
			assertEquals( 1, p.getChildren().size() );
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue
		private int id;

		private String name;

		@OneToMany(mappedBy = "parent")
		private List<Child> children = new LinkedList<Child>();

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue
		private int id;

		private String name;

		@ManyToOne
		private Parent parent;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "ParentCascade")
	public static class ParentCascade {

		@Id
		@GeneratedValue
		private Integer id;

		@OneToMany(mappedBy = "parent", cascade = { CascadeType.ALL })
		private List<ChildCascade> children = new ArrayList<ChildCascade>();

		public Integer getId() {
			return id;
		}

		public List<ChildCascade> getChildren() {
			return children;
		}

		public void setChildren(List<ChildCascade> children) {
			this.children = children;
		}
	}

	@Entity(name = "ChildCascade")
	public static class ChildCascade {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		private ParentCascade parent;

		public Integer getId() {
			return id;
		}

		public ParentCascade getParent() {
			return parent;
		}

		public void setParent(ParentCascade parent) {
			this.parent = parent;
		}
	}
}
