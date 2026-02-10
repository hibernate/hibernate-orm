/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses =
		{JoinFormulaCompositeBidiTest.Parent.class,
		JoinFormulaCompositeBidiTest.Child.class})
@JiraKey("HHH-15492")
class JoinFormulaCompositeBidiTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			var parent = new Parent();
			parent.name = "Parent";
			parent.region = 'A';
			em.persist(parent);
			var child = new Child();
			child.parentId = parent.id;
			child.region = 'A';
			parent.children.add(child);
			em.persist(child);
		} );
		scope.inTransaction( em -> {
			Parent parent = em.find( Parent.class, new ParentId(1L, 'A') );
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
	record ParentId(long id, char region) {}
	@Entity(name = "Parent")
	@IdClass(ParentId.class)
	static class Parent {
		@Id @GeneratedValue
		long id;
		String name;
		@Id
		char region;
		@OneToMany(mappedBy = "parent")
		Set<Child> children = new HashSet<>();
	}
	@Entity(name = "Child")
	static class Child {
		@Id
		long id;
		long parentId;
		char region;
		@ManyToOne
		@JoinColumnOrFormula(formula = @JoinFormula("abs(parentId)"))
		@JoinColumnOrFormula(column = @JoinColumn(name = "region",
				insertable = false, updatable = false))
		Parent parent;
	}
}
