/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.Immutable;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_ONLY;

@Jpa(annotatedClasses = CachedReadOnlyArrayTest.Publication.class)
class CachedReadOnlyArrayTest {

	@Test
	void testReadFromCache(EntityManagerFactoryScope scope) {
		scope.inTransaction(em -> {
			Publication entity1 = new Publication();
			entity1.id = "123l";
			em.persist(entity1);
		});
		scope.inTransaction(em -> em.find(Publication.class, "123l"/*, ReadOnlyMode.READ_ONLY*/));

	}

	@Immutable
	@Entity
	@Cache(usage = READ_ONLY)
	static class Publication {
		@Id
		String id;
		@ElementCollection
		String[] topics;
	}
}
