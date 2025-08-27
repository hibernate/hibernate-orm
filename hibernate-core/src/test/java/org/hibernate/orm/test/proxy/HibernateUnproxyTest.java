/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy;

import java.util.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = { HibernateUnproxyTest.Parent.class, HibernateUnproxyTest.Child.class }
)
public class HibernateUnproxyTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope){
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInitializedProxyCanBeUnproxied(EntityManagerFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		p.setChild( c );

		scope.inTransaction( entityManager ->
									entityManager.persist( p )
		);

		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, p.getId() );
			Child child = parent.getChild();

			assertFalse( Hibernate.isInitialized( child ) );
			Hibernate.initialize( child );

			Child unproxiedChild = (Child) Hibernate.unproxy( child );
			assertEquals( Child.class, unproxiedChild.getClass() );
		} );

		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, p.getId() );
			Child child = parent.getChild();

			assertFalse( Hibernate.isInitialized( child ) );
			Hibernate.initialize( child );

			Child unproxiedChild = Hibernate.unproxy( child, Child.class );

			assertEquals( Child.class, unproxiedChild.getClass() );
		} );
	}

	@Test
	public void testNotInitializedProxyCanBeUnproxiedWithInitialization(EntityManagerFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		p.setChild( c );

		scope.inTransaction( entityManager ->
									entityManager.persist( p )
		);

		scope.inTransaction( entityManager -> {

			Parent parent = entityManager.find( Parent.class, p.getId() );
			Child child = parent.getChild();

			assertFalse( Hibernate.isInitialized( child ) );

			Child unproxiedChild = (Child) Hibernate.unproxy( child );

			assertTrue( Hibernate.isInitialized( child ) );
			assertEquals( Child.class, unproxiedChild.getClass() );
		} );

		scope.inTransaction( entityManager -> {

			Parent parent = entityManager.find( Parent.class, p.getId() );
			Child child = parent.getChild();

			assertFalse( Hibernate.isInitialized( child ) );

			Child unproxiedChild = Hibernate.unproxy( child, Child.class );

			assertTrue( Hibernate.isInitialized( child ) );
			assertEquals( Child.class, unproxiedChild.getClass() );
		} );
	}

	@Test
	public void testNotHibernateProxyShouldThrowException(EntityManagerFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		p.setChild( c );

		scope.inTransaction( entityManager ->
									entityManager.persist( p )
		);

		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, p.getId() );
			assertSame( parent, Hibernate.unproxy( parent ) );
		} );

		scope.inTransaction( entityManager -> {
			Parent parent = entityManager.find( Parent.class, p.getId() );
			assertSame( parent, Hibernate.unproxy( parent, Parent.class ) );
		} );
	}

	@Test
	public void testNullUnproxyReturnsNull() {
		assertNull( Hibernate.unproxy( null ) );

		assertNull( Hibernate.unproxy( null, Parent.class ) );
	}

	@Test
	public void testProxyEquality(EntityManagerFactoryScope scope) {
		Parent parent = scope.fromTransaction( entityManager -> {
			Parent p = new Parent();
			p.name = "John Doe";
			entityManager.persist( p );
			return p;
		} );

		scope.inTransaction( entityManager -> {
			Parent p = entityManager.getReference( Parent.class, parent.getId() );
			assertFalse( parent.equals( p ) );
			assertTrue( parent.equals( Hibernate.unproxy( p ) ) );
		} );

		scope.inTransaction( entityManager -> {
			Parent p = entityManager.getReference( Parent.class, parent.getId() );
			assertFalse( parent.equals( p ) );
			assertTrue( parent.equals( Hibernate.unproxy( p, Parent.class ) ) );
		} );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Child child;

		public Integer getId() {
			return id;
		}

		public void setChild(Child child) {
			this.child = child;
			child.setParent( this );
		}

		public Child getChild() {
			return child;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Parent parent = (Parent) o;
			return Objects.equals( name, parent.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name );
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		@OneToOne(fetch = FetchType.LAZY)
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public Parent getParent() {
			return parent;
		}
	}
}
