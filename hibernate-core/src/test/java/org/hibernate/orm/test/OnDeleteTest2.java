/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.RollbackException;
import org.hibernate.TransientObjectException;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Jpa(annotatedClasses = {OnDeleteTest2.Parent.class, OnDeleteTest2.Child.class})
public class OnDeleteTest2 {
	@Test
	public void testOnDeleteParent(EntityManagerFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
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
			// since it's an owned collection, the FK gets set to null
			assertNotNull( em.find( Child.class, child.id ) );
		} );
	}

	@Test
	public void testOnDeleteChildrenFails(EntityManagerFactoryScope scope) {
		Parent parent = new Parent();
		Child child = new Child();
		parent.children.add( child );
		scope.inTransaction( em -> {
			em.persist( parent );
			em.persist( child );
		} );
		try {
			scope.inTransaction( em -> {
				Parent p = em.find( Parent.class, parent.id );
				for ( Child c : p.children ) {
					em.remove( c );
				}
			} );
			fail();
		}
		catch (RollbackException re) {
			assertTrue(re.getCause().getCause() instanceof TransientObjectException);
		}
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Entity
	static class Parent {
		@Id
		long id;
		@OneToMany(fetch = EAGER)
		@JoinColumn(name = "parent_id")
		@OnDelete(action = OnDeleteAction.CASCADE)
		Set<Child> children = new HashSet<>();
	}

	@Entity
	static class Child {
		@Id
		long id;
	}
}
