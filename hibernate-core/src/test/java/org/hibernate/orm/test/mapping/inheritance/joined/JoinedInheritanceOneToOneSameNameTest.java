/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinedInheritanceOneToOneSameNameTest.Parent.class,
		JoinedInheritanceOneToOneSameNameTest.Child.class,
		JoinedInheritanceOneToOneSameNameTest.ChildA.class,
		JoinedInheritanceOneToOneSameNameTest.ChildB.class,
		JoinedInheritanceOneToOneSameNameTest.Something.class
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17104" )
public class JoinedInheritanceOneToOneSameNameTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new Parent( 1L, null ) );
			final ChildA childA = new ChildA( new Something() );
			session.persist( childA );
			session.persist( new Parent( 2L, childA ) );
			final ChildB childB = new ChildB( new Something() );
			session.persist( childB );
			session.persist( new Parent( 3L, childB ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Parent" ).executeUpdate();
			session.createMutationQuery( "delete from Child" ).executeUpdate();
			session.createMutationQuery( "delete from Something" ).executeUpdate();
		} );
	}

	@Test
	public void testFindParentById(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Parent parent = session.find( Parent.class, 1L );
			assertThat( parent ).isNotNull();
		} );
	}

	@Test
	public void testFindParentWithChildById(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Parent parent = session.find( Parent.class, 2L );
			assertThat( parent ).isNotNull();
			assertThat( parent.getChild() ).isInstanceOf( ChildA.class );
			assertThat( ( (ChildA) parent.getChild() ).getSomething() ).isNotNull();
		} );
	}

	@Test
	public void testQueryParentById(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Parent parent = session.createQuery( "select p from Parent p where p.id = :id", Parent.class )
					.setParameter( "id", 1L )
					.getSingleResult();
			assertThat( parent ).isNotNull();
		} );
	}

	@Test
	public void testQueryParentWithChildById(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Parent parent = session.createQuery( "select p from Parent p where p.id = :id", Parent.class )
					.setParameter( "id", 3L )
					.getSingleResult();
			assertThat( parent ).isNotNull();
			assertThat( parent.getChild() ).isInstanceOf( ChildB.class );
			assertThat( ( (ChildB) parent.getChild() ).getSomething() ).isNotNull();
		} );
	}

	@Entity( name = "Parent" )
	public static class Parent {
		@Id
		private Long id;

		@OneToOne
		private Child child;

		public Parent() {
		}

		public Parent(Long id, Child child) {
			this.id = id;
			this.child = child;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity( name = "Child" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity( name = "ChildA" )
	public static class ChildA extends Child {
		@OneToOne( cascade = CascadeType.ALL )
		private Something something;

		public ChildA() {
		}

		public ChildA(Something something) {
			this.something = something;
		}

		public Something getSomething() {
			return something;
		}
	}

	@Entity( name = "ChildB" )
	public static class ChildB extends Child {
		@OneToOne( cascade = CascadeType.ALL )
		private Something something;

		public ChildB() {
		}

		public ChildB(Something something) {
			this.something = something;
		}

		public Something getSomething() {
			return something;
		}
	}

	@Entity( name = "Something" )
	public static class Something {
		@Id
		@GeneratedValue
		private Long id;
	}
}
