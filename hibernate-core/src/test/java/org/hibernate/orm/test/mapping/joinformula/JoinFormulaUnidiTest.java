/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.*;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = JoinFormulaUnidiTest.Thing.class)
@JiraKey("HHH-12997")
class JoinFormulaUnidiTest {

	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Thing rootThing = new Thing();
			rootThing.name = "a";
			entityManager.persist( rootThing );

			Thing childThing1 = new Thing();
			childThing1.name = "b";
			childThing1.parentName = "A";
			entityManager.persist( childThing1 );

			Thing childThing2 = new Thing();
			childThing2.name = "c";
			childThing2.parentName = "A";
			entityManager.persist( childThing2 );
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "a");
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing =
					entityManager.createQuery("from Thing t join fetch childThings where t.name = 'a'", Thing.class)
							.getSingleResult();
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "a");
			Thing childThing = entityManager.find( Thing.class, "b");
			rootThing.childThings.remove( childThing );
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "a");
			assertEquals(2, rootThing.childThings.size());
		} );
		scope.inTransaction( entityManager -> {
			Thing childThing = entityManager.find( Thing.class, "b");
			childThing.parentName = null;
		} );
		scope.inTransaction( entityManager -> {
			Thing rootThing = entityManager.find( Thing.class, "a");
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
