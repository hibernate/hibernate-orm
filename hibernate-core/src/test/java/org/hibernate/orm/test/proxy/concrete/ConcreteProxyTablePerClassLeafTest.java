/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Test for HHH-20798: getReference() throws NullPointerException when called for
 * a leaf entity (no subclasses) in a TABLE_PER_CLASS inheritance hierarchy with @ConcreteProxy.
 *
 * @author Chikumar Nagappa
 */
@DomainModel(
		annotatedClasses = {
				ConcreteProxyTablePerClassLeafTest.Parent.class,
				ConcreteProxyTablePerClassLeafTest.Child.class,
				ConcreteProxyTablePerClassLeafTest.Holder.class
		}
)
@SessionFactory
@BytecodeEnhanced(runNotEnhancedAsWell = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-20798")
public class ConcreteProxyTablePerClassLeafTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Child child = new Child( 1L, "test child" );
			session.persist( child );

			Holder holder = new Holder( 1L, child );
			session.persist( holder );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Holder" ).executeUpdate();
			session.createMutationQuery( "delete from Child" ).executeUpdate();
		} );
	}

	@Test
	public void testGetReferenceOnLeafEntity(SessionFactoryScope scope) {
		// This should not throw NullPointerException
		scope.inSession( session -> {
			// Test getReference on leaf entity (Child has no subclasses)
			Child proxy = session.getReference( Child.class, 1L );
			assertThat( proxy, notNullValue() );
			assertThat( proxy, instanceOf( Child.class ) );
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );

			// Access a property to force initialization
			String name = proxy.getName();
			assertThat( name, is( "test child" ) );
			assertThat( Hibernate.isInitialized( proxy ), is( true ) );
		} );
	}

	@Test
	public void testGetReferenceOnParentEntity(SessionFactoryScope scope) {
		// This should work as Parent has a subclass (Child)
		scope.inSession( session -> {
			Parent proxy = session.getReference( Parent.class, 1L );
			assertThat( proxy, notNullValue() );
			assertThat( proxy, instanceOf( Child.class ) );
			assertThat( Hibernate.isInitialized( proxy ), is( false ) );
		} );
	}

	@Test
	public void testManyToOneWithLeafEntity(SessionFactoryScope scope) {
		// Test lazy loading of a many-to-one association to a leaf entity
		scope.inSession( session -> {
			Holder holder = session.find( Holder.class, 1L );
			assertThat( holder, notNullValue() );

			Child child = holder.getChild();
			assertThat( child, notNullValue() );
			assertThat( child, instanceOf( Child.class ) );
			assertThat( Hibernate.isInitialized( child ), is( false ) );

			// Access property to verify proxy works
			String name = child.getName();
			assertThat( name, is( "test child" ) );
		} );
	}

	@Entity(name = "Parent")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@ConcreteProxy
	public static class Parent {
		@Id
		private Long id;

		private String name;

		public Parent() {
		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Child")
	public static class Child extends Parent {
		private String childProperty;

		public Child() {
		}

		public Child(Long id, String name) {
			super( id, name );
			this.childProperty = "child-" + id;
		}

		public String getChildProperty() {
			return childProperty;
		}
	}

	@Entity(name = "Holder")
	public static class Holder {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Child child;

		public Holder() {
		}

		public Holder(Long id, Child child) {
			this.id = id;
			this.child = child;
		}

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}
	}
}
