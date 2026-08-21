/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.merge;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = {OwnedCollectionMergeTest.Child.class,
		OwnedCollectionMergeTest.Parent.class})
class OwnedCollectionMergeTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Parent parent = new Parent();
			em.persist( parent );
			Child child = new Child();
			parent.children = new HashSet<>();
			parent.children.add( child );
			em.persist( child );
		} );
		Parent p = scope.fromTransaction( em -> {
			return em.find( Parent.class, 1 );
		} );
		assertFalse( isInitialized( p.children ) );
		scope.inTransaction( em -> {
			Parent parent = em.merge( p );
			assertFalse( isInitialized( parent.children ) );
		} );
		p.children = null;
		scope.inTransaction( em -> {
			Parent parent = em.merge( p );
			assertTrue( isInitialized( parent.children ) );
			assertTrue( parent.children.isEmpty() );
		} );
	}

	@Entity(name="Parent")
	static class Parent {
		@Id
		@GeneratedValue
		Long id;

		@OneToMany(fetch = FetchType.LAZY)
		Set<Child> children;
	}

	@Entity(name="Child")
	static class Child {
		@Id
		@GeneratedValue
		Long id;
	}
}
