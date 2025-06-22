/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = StatelessCollectionsTest.WithCollection.class)
public class StatelessCollectionsTest {
	@Test
	void test(SessionFactoryScope scope) {
		WithCollection inserted = new WithCollection();
		inserted.name = "Gavin";
		inserted.elements.add("Hello");
		inserted.elements.add("World");
		scope.inStatelessTransaction(s -> s.insert(inserted));

		scope.inStatelessTransaction(s -> {
			WithCollection loaded = s.get(WithCollection.class, inserted.id);
			assertFalse(isInitialized(loaded.elements));
			s.fetch(loaded.elements);
			assertTrue(isInitialized(loaded.elements));
			assertEquals(2, loaded.elements.size());

			loaded.elements.add("Goodbye");
			s.update(loaded);
		});

		scope.inStatelessTransaction(s -> {
			WithCollection loaded = s.get(WithCollection.class, inserted.id);
			assertFalse(isInitialized(loaded.elements));
			s.fetch(loaded.elements);
			assertTrue(isInitialized(loaded.elements));
			assertEquals(3, loaded.elements.size());

			loaded.elements.remove("Hello");
			s.update(loaded);
		});

		scope.inStatelessTransaction(s -> {
			WithCollection loaded = s.get(WithCollection.class, inserted.id);
			assertFalse(isInitialized(loaded.elements));
			s.fetch(loaded.elements);
			assertTrue(isInitialized(loaded.elements));
			assertEquals(2, loaded.elements.size());

			s.delete(loaded);
		});

		scope.inStatelessTransaction(s -> {
			WithCollection loaded = s.get(WithCollection.class, inserted.id);
			assertNull(loaded);
		});
	}

	@Entity(name = "EntityWithCollection")
	static class WithCollection {
		@GeneratedValue @Id
		Long id;
		String name;
		@ElementCollection
		Set<String> elements = new HashSet<>();
	}
}
