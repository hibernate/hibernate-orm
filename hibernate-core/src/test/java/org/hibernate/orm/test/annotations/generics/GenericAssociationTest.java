/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Yoann Rodière
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericAssociationTest.AbstractParent.class,
		GenericAssociationTest.Parent.class,
		GenericAssociationTest.AbstractChild.class,
		GenericAssociationTest.Child.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16378" )
public class GenericAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			final Child child = new Child( 2L );
			child.setParent( parent );
			parent.getChildren().add( child );
			session.persist( parent );
			session.persist( child );
		} );
	}

	@Test
	public void testGenericParentQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select parent.id from Child",
				Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}

	@Test
	public void testGenericChildQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select c.id from Parent p join p.children c",
				Long.class
		).getSingleResult() ).isEqualTo( 2L ) );
	}

	@MappedSuperclass
	public abstract static class AbstractParent<T> {
		@OneToMany
		private List<T> children;

		public AbstractParent() {
			this.children = new ArrayList<>();
		}

		public List<T> getChildren() {
			return children;
		}

		public void setChildren(List<T> children) {
			this.children = children;
		}
	}

	@Entity( name = "Parent" )
	public static class Parent extends AbstractParent<Child> {
		@Id
		private Long id;

		public Parent() {
		}

		public Parent(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public abstract static class AbstractChild<T> {
		@ManyToOne
		private T parent;

		public AbstractChild() {
		}

		public abstract Long getId();

		public T getParent() {
			return this.parent;
		}

		public void setParent(T parent) {
			this.parent = parent;
		}
	}

	@Entity( name = "Child" )
	public static class Child extends AbstractChild<Parent> {
		@Id
		protected Long id;

		public Child() {
		}

		public Child(Long id) {
			this.id = id;
		}

		@Override
		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
