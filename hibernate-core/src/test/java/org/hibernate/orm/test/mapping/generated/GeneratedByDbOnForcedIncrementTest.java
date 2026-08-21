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
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hibernate.annotations.SourceType.DB;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = GeneratedByDbOnForcedIncrementTest.WithUpdateTimestamp.class)
@SkipForDialect(dialectClass = InformixDialect.class,
		reason = "JDBC driver returns timestamp with seconds precision")
class GeneratedByDbOnForcedIncrementTest {
	@Test void test(EntityManagerFactoryScope scope) throws InterruptedException {
		var persisted = scope.fromTransaction( em -> {
			var entity = new WithUpdateTimestamp();
			em.persist( entity );
			return entity;
		} );
		Thread.sleep( 100 );
		var updated = scope.fromTransaction( em -> {
			var entity = em.find( WithUpdateTimestamp.class, 0L );
			entity.names.add( "Gavin" );
			return entity;
		} );
		assertTrue( persisted.updated.isBefore( updated.updated ) );
	}
	@Entity
	static class WithUpdateTimestamp {
		@Id long id;
		@Version long version;
		@UpdateTimestamp(source = DB)
		LocalDateTime updated;
		@ElementCollection
		Set<String> names;
	}
}
