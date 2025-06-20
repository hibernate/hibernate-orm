/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.eviction;

import org.hibernate.engine.internal.ManagedTypeHelper;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@JiraKey("HHH-17464")
@DomainModel(
		annotatedClasses = {
				CascadeAllEvictTest.Parent.class,
				CascadeAllEvictTest.Child.class
		}
)
@SessionFactory
public class CascadeAllEvictTest {

	private static final Long PARENT_ID = 1L;
	private static final Long CHILD_ID = 2L;


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child( CHILD_ID, "child" );
					Parent parent = new Parent( PARENT_ID, "parent", child );
					session.persist( parent );
					session.persist( child );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEvict(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					ManagedTypeHelper.asHibernateProxy( parent.child )
							.getHibernateLazyInitializer()
							.isUninitialized();
					session.evict( parent );
				}
		);
	}

	@Test
	public void testEvictChildAssociationBeforeParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					session.get( Child.class, CHILD_ID );
					ManagedTypeHelper.asHibernateProxy( parent.child )
							.getHibernateLazyInitializer()
							.isUninitialized();
					session.evict( parent.child );
					session.evict( parent );
				}
		);
	}

	@Test
	public void testEvictParentAndChildAssociation(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = session.get( Parent.class, PARENT_ID );
					session.get( Child.class, CHILD_ID );
					ManagedTypeHelper.asHibernateProxy( parent.child )
							.getHibernateLazyInitializer()
							.isUninitialized();
					session.evict( parent );
					session.evict( parent.child );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToOne(fetch = LAZY, cascade = ALL, orphanRemoval = true)
		Child child;

		public Parent() {
		}

		public Parent(Long id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		Long id;

		private String name;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

}
