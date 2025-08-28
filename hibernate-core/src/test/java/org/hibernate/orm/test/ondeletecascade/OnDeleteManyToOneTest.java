/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ondeletecascade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.OnDelete;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.annotations.OnDeleteAction.CASCADE;
import static org.junit.jupiter.api.Assertions.assertNull;

@Jpa(annotatedClasses = {OnDeleteManyToOneTest.Parent.class, OnDeleteManyToOneTest.Child.class})
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCascadeDeleteCheck.class)
public class OnDeleteManyToOneTest {
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
		scope.inTransaction( em -> em.remove( em.getReference( parent ) ) );
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
		scope.getEntityManagerFactory().getSchemaManager().truncate();
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
