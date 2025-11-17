/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.EAGER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = EagerCollectionInStatelessTest.WithEagerCollection.class)
public class EagerCollectionInStatelessTest {
	@Test
	void test(SessionFactoryScope scope) {
		scope.inStatelessTransaction(s-> {
			WithEagerCollection entity = new WithEagerCollection();
			entity.eager.add("Hello");
			entity.eager.add("World");
			entity.lazy.add("Goodbye");
			entity.lazy.add("World");
			s.insert(entity);
		});
		scope.inStatelessSession(s-> {
			WithEagerCollection entity = s.get(WithEagerCollection.class, 69L);
			assertTrue(Hibernate.isInitialized(entity.eager));
			assertFalse(Hibernate.isInitialized(entity.lazy));
			s.fetch(entity.lazy);
			assertTrue(Hibernate.isInitialized(entity.lazy));
			assertEquals(2, entity.eager.size());
			assertEquals(2, entity.lazy.size());
		});
		scope.inStatelessSession(s-> {
			WithEagerCollection entity =
					s.createSelectionQuery("where id= 69L", WithEagerCollection.class)
							.getSingleResult();
			assertTrue(Hibernate.isInitialized(entity.eager));
			assertFalse(Hibernate.isInitialized(entity.lazy));
			s.fetch(entity.lazy);
			assertTrue(Hibernate.isInitialized(entity.lazy));
			assertEquals(2, entity.eager.size());
			assertEquals(2, entity.lazy.size());
		});
	}

	@Entity
	static class WithEagerCollection {
		@Id long id = 69L;
		@ElementCollection(fetch = EAGER)
		Set<String> eager = new HashSet<>();
		@ElementCollection
		Set<String> lazy = new HashSet<>();
	}
}
