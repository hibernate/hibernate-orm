/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.*;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = JoinFormulaUnidiTest.Thing.class)
class JoinFormulaUnidiTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Thing rootThing = new Thing();
			rootThing.name = "0";
			entityManager.persist( rootThing );

			Thing childThing1 = new Thing();
			childThing1.name = "1";
			childThing1.parentName = "0";
			entityManager.persist( childThing1 );

			Thing childThing2 = new Thing();
			childThing2.name = "2";
			childThing2.parentName = "0";
			entityManager.persist( childThing2 );
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "0");
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing =
					entityManager.createQuery("from Thing t join fetch childThings where t.name = '0'", Thing.class)
							.getSingleResult();
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "0");
			Thing childThing = entityManager.find( Thing.class, "1");
			rootThing.childThings.remove( childThing );
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "0");
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing childThing = entityManager.find( Thing.class, "1");
			childThing.parentName = null;
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "0");
			assertEquals(1, rootThing.childThings.size());
		} );
	}

	@Entity(name = "Thing")
	public static class Thing {

		@Id
		private String name;

		@Column(name = "parent_name")
		private String parentName;

		@OneToMany(fetch = FetchType.LAZY)
		@JoinFormula(value = "lower(parent_name)",
				referencedColumnName = "name")
		private Set<Thing> childThings = new HashSet<>();

	}
}
