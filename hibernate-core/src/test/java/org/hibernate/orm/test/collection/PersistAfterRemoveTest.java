/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				PersistAfterRemoveTest.Parent.class,
				PersistAfterRemoveTest.Child.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-4468")
public class PersistAfterRemoveTest {

	private static final Integer PARENT_ID = 7242000;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent parent = new Parent( PARENT_ID, "Test" );
			parent.addChild( new Child( 1 ) );
			parent.addChild( new Child( 2 ) );
			session.persist( parent );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Parent foundParent = session.find( Parent.class, PARENT_ID );
			session.remove( foundParent );
			session.getTransaction().commit();
			session.getTransaction().begin();
			session.persist( foundParent );
		} );

		scope.inTransaction( session -> {
			Parent foundParent = session.find( Parent.class, PARENT_ID );
			Set<Child> children = foundParent.getChildren();
			assertThat( children.size() ).isEqualTo( 2 );
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "parent_table")
	static class Parent {
		@Id
		private Integer id;
		private String name;

		@OneToMany(mappedBy="parent", cascade = CascadeType.ALL)
		private Set<Child> children = new HashSet<>();

		public Parent() {
		}

		public Parent(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Child> getChildren() {
			return children;
		}

		public void setChildren(Set<Child> children) {
			this.children = children;
		}

		public void addChild(Child child) {
			children.add( child );
			child.parent = this;
		}

	}

	@Entity(name = "Child")
	@Table(name = "child_table")
	public static class Child {

		@Id
		private Integer id;
		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Integer id) {
			this.id = id;
		}
	}
}
