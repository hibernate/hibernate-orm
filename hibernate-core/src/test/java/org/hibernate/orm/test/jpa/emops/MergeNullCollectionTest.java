/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = MergeNullCollectionTest.Thing.class)
class MergeNullCollectionTest {
	@Test void test(EntityManagerFactoryScope scope) {
		Thing thing = new Thing();
		thing.strings.add( "hello" );
		thing.strings.add( "goodbye" );
		scope.inTransaction( em -> {
			em.persist( thing );
		} );
		scope.inTransaction( em -> {
			assertEquals(2, em.find( Thing.class, thing.id ).strings.size());
		});
		thing.strings = null;
		scope.inTransaction( em -> {
			assertEquals( 0, em.merge( thing ).strings.size() );
		} );
		scope.inTransaction( em -> {
			assertEquals(0, em.find( Thing.class, thing.id ).strings.size());
		});
	}
	@Entity(name = "Thing")
	static class Thing {
		@Id
		long id;
		@ElementCollection
		Set<String> strings = new HashSet<>();
	}
}
