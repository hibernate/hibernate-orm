/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;



@SessionFactory
@DomainModel(annotatedClasses = {
		OneToManyMappedByOrderColumnTest.Parent.class,
		OneToManyMappedByOrderColumnTest.Child.class
})
public class OneToManyMappedByOrderColumnTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testReadNullIndex(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = new Parent();
			parent.setId( 1 );
			s.persist( parent );

			Child child = new Child();
			child.setId( 2 );
			child.setParent( parent );
			s.persist( child );
		} );

		scope.inTransaction( s -> {
			try {
				s.get( Parent.class, 1 );
				Assertions.fail( "Expected to fail because list index is null" );
			}
			catch (HibernateException ex) {
				Assertions.assertTrue( ex.getMessage().contains( "children" ) );
			}
		} );
	}

	@Entity(name = "parent")
	public static class Parent {

		@Id
		private Integer id;
		@OrderColumn(name = "list_idx")
		@OneToMany(targetEntity = Child.class, mappedBy = "parent", fetch = FetchType.EAGER)
		@Cascade(CascadeType.REMOVE)
		private List<Child> children = new ArrayList<>();

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

	@Entity(name = "child")
	public static class Child {

		@Id
		private Integer id;
		@ManyToOne(targetEntity = Parent.class, fetch = FetchType.LAZY)
		private Parent parent;

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
	}
}
