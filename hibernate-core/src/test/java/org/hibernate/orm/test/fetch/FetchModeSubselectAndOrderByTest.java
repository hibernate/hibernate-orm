/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(
		annotatedClasses = {
				FetchModeSubselectAndOrderByTest.Parent.class,
				FetchModeSubselectAndOrderByTest.Child.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16202")
public class FetchModeSubselectAndOrderByTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( "Filippo" );

					Child child = new Child( parent, "Luigi" );
					Child child2 = new Child( parent, "And" );
					Child child3 = new Child( parent, "Mary" );

					session.persist( child );
					session.persist( child2 );
					session.persist( child3 );

				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Parent> parents = session.createQuery( "select p from Parent p", Parent.class ).list();

					assertThat( parents.size() ).isEqualTo( 1 );

					Parent parent = parents.get( 0 );
					List<Child> children = parent.getChildren();

					assertThat( children.size() ).isEqualTo( 3 );
					assertThat( children.get( 0 ).getName() ).isEqualTo( "And" );
					assertThat( children.get( 1 ).getName() ).isEqualTo( "Luigi" );
					assertThat( children.get( 2 ).getName() ).isEqualTo( "Mary" );
				}
		);
	}


	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		@Fetch(value = FetchMode.SUBSELECT)
		@OrderBy("name ASC")
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(String name) {
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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
		private Integer id;

		@ManyToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "parent_id", nullable = false)
		private Parent parent;

		private String name;

		public Child(Parent parent, String name) {
			this.parent = parent;
			parent.children.add( this );
			this.name = name;
		}

		public Child() {
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
