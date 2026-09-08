/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = GeneratedOnForcedIncrementTest.WithUpdateTimestamp.class)
class GeneratedOnForcedIncrementTest {
	@Test void test(EntityManagerFactoryScope scope) {
		var persisted = scope.fromTransaction( em -> {
			var entity = new WithUpdateTimestamp();
			em.persist( entity );
			return entity;
		} );
		DialectContext.awaitTimestampTick();
		var updated = scope.fromTransaction( em -> {
			var entity = em.find( WithUpdateTimestamp.class, 0L );
			entity.names.add( "Gavin" );
			return entity;
		} );
		assertTrue( persisted.updated.isBefore( updated.updated ) );
	}
	@Entity(name = "WithUpdateTimestamp")
	static class WithUpdateTimestamp {
		@Id long id;
		@Version long version;
		@UpdateTimestamp
		LocalDateTime updated;
		@ElementCollection
		Set<String> names;
	}
}
