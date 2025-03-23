/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/**
 * @author Yoann Rodière
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericToOneAssociationTest.AbstractParent.class,
		GenericToOneAssociationTest.Parent.class,
		GenericToOneAssociationTest.AbstractChild.class,
		GenericToOneAssociationTest.Child.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16378" )
public class GenericToOneAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent parent = new Parent( 1L );
			session.persist( parent );
			final Child child = new Child( 2L );
			child.setParent( parent );
			parent.setChild( child );
			session.persist( child );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createQuery( "from Parent", Parent.class )
				.getResultList()
				.forEach( p -> p.setChild( null ) ) );
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Child" ).executeUpdate();
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void testParentQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select parent.id from Child",
				Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}

	@Test
	public void testParentCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> query = cb.createQuery( Long.class );
			final Root<Child> root = query.from( Child.class );
			final Path<Parent> parent = root.get( "parent" );
			// generic attributes are always reported as Object java type
			assertThat( parent.getJavaType() ).isEqualTo( Object.class );
			assertThat( parent.getModel() ).isSameAs( root.getModel().getAttribute( "parent" ) );
			assertThat( ( (SqmPath<?>) parent ).getResolvedModel().getBindableJavaType() ).isEqualTo( Parent.class );
			final Long result = session.createQuery( query.select( parent.get( "id" ) ) ).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testChildQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select c.id from Parent p join p.child c",
				Long.class
		).getSingleResult() ).isEqualTo( 2L ) );
	}

	@Test
	public void testChildCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> query = cb.createQuery( Long.class );
			final Root<Parent> root = query.from( Parent.class );
			final Join<Parent, Child> join = root.join( "child" );
			// generic attributes are always reported as Object java type
			assertThat( join.getJavaType() ).isEqualTo( Object.class );
			assertThat( join.getModel() ).isSameAs( root.getModel().getAttribute( "child" ) );
			assertThat( ( (SqmPath<?>) join ).getResolvedModel().getBindableJavaType() ).isEqualTo( Child.class );
			final Long result = session.createQuery( query.select( join.get( "id" ) ) ).getSingleResult();
			assertThat( result ).isEqualTo( 2L );
		} );
	}

	@MappedSuperclass
	public abstract static class AbstractParent<T> {
		@OneToOne
		private T child;

		public T getChild() {
			return child;
		}

		public void setChild(T child) {
			this.child = child;
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
	}

	@MappedSuperclass
	public abstract static class AbstractChild<T> {
		@OneToOne(mappedBy = "child")
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
	}
}
