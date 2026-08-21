/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses =
		{JoinFormulaBidiTest.Parent.class,
		JoinFormulaBidiTest.Child.class})
@JiraKey("HHH-15492")
class JoinFormulaBidiTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			var parent = new Parent();
			parent.name = "Parent";
			em.persist(parent);
			var child = new Child();
			child.parentId = parent.id;
			parent.children.add(child);
			em.persist(child);
		} );
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, 1L );
			assertEquals(1, parent.children.size());
			assertEquals(1L, parent.children.iterator().next().parentId);
		} );
		scope.inTransaction( em -> {
			Parent parent =
					em.createQuery( "from Parent p join fetch p.children where p.id = 1", Parent.class )
							.getSingleResult();
			assertEquals(1, parent.children.size());
			assertEquals(1L, parent.children.iterator().next().parentId);
		} );
	}
	@Entity(name = "Parent")
	static class Parent {
		@Id @GeneratedValue
		long id;
		String name;
		@OneToMany(mappedBy = "parent")
		Set<Child> children = new HashSet<>();
	}
	@Entity(name = "Child")
	static class Child {
		@Id
		long id;
		long parentId;
		@ManyToOne
		@JoinFormula( "abs(parentId)" )
		Parent parent;
	}
}
