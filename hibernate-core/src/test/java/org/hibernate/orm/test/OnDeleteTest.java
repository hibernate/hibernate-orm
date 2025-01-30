/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.OnDelete;
import org.hibernate.internal.SessionFactoryImpl;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.annotations.OnDeleteAction.CASCADE;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(annotatedClasses = {OnDeleteTest.Parent.class, OnDeleteTest.Child.class})
public class OnDeleteTest {
	@Test
	public void testOnDelete(EntityManagerFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		child.parent = parent;
		parent.children.add( child );
		scope.inTransaction( em -> {
			em.persist( parent );
			em.persist( child );
		} );
		scope.inTransaction( em -> {
			Parent p = em.find( Parent.class, parent.id );
			em.remove( p );
		} );
		scope.inTransaction( em -> {
			assertNull( em.find( Child.class, child.id ) );
		} );
	}

	@Test
	public void testOnDeleteReference(EntityManagerFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		child.parent = parent;
		parent.children.add( child );
		scope.inTransaction( em -> {
			em.persist( parent );
			em.persist( child );
		} );
		scope.inTransaction( em -> em.remove( em.getReference( Parent.class, parent.id ) ) );
		scope.inTransaction( em -> assertNull( em.find( Child.class, child.id ) ) );
	}

	@Test
	public void testOnDeleteInReverse(EntityManagerFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		child.parent = parent;
		parent.children.add( child );
		scope.inTransaction( em -> {
			em.persist( parent );
			em.persist( child );
		} );
		scope.inTransaction( em -> {
			Child c = em.find( Child.class, child.id );
			em.remove( c );
		} );
		scope.inTransaction( em -> {
			assertNull( em.find( Child.class, child.id ) );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactoryImpl.class ).getSchemaManager().truncateMappedObjects();
	}

	@Entity
	static class Parent {
		@Id
		long id;
		@OneToMany(mappedBy = "parent", fetch = EAGER)
		Set<Child> children = new HashSet<>();
	}

	@Entity
	static class Child {
		@Id
		long id;
		@ManyToOne
		@OnDelete(action = CASCADE)
		Parent parent;
	}
}
