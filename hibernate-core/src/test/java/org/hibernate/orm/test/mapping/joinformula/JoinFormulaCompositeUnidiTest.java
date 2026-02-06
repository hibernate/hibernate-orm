/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
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

@Jpa(annotatedClasses = JoinFormulaCompositeUnidiTest.Thing.class)
@JiraKey("HHH-12997")
class JoinFormulaCompositeUnidiTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Thing rootThing = new Thing();
			rootThing.name = "0";
			rootThing.depth = 0;

			Thing childThing1 = new Thing();
			childThing1.name = "1";
			childThing1.depth = 1;
			rootThing.childThings.add( childThing1 );

			Thing childThing2 = new Thing();
			childThing2.name = "2";
			childThing2.depth = 1;
			rootThing.childThings.add( childThing2 );

			entityManager.persist( rootThing );
			entityManager.persist( childThing1 );
			entityManager.persist( childThing2 );
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find(Thing.class, new ParentId("0", 0));
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing =
					entityManager.createQuery("from Thing t join fetch childThings where t.name = '0'", Thing.class)
							.getSingleResult();
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find(Thing.class, new ParentId("0", 0));
			Thing childThing = entityManager.find(Thing.class, new ParentId("1", 1));
			rootThing.childThings.remove( childThing );
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find(Thing.class, new ParentId("0", 0));
			assertEquals(1, rootThing.childThings.size());
		} );
	}

	record ParentId(String name, int depth) {}

	@Entity(name = "Thing")
	@IdClass(ParentId.class)
	public static class Thing {

		@Id
		String name;
		@Id
		int depth;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinColumnOrFormula(column = @JoinColumn(name = "parent_name",
				referencedColumnName = "name"))
		@JoinColumnOrFormula(formula = @JoinFormula(value = "depth-1",
				referencedColumnName = "depth"))
		Set<Thing> childThings = new HashSet<>();

	}
}
