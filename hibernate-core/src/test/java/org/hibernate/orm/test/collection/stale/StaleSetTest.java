/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.stale;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;

@Jpa(annotatedClasses = StaleSetTest.StaleSetTestEntity.class,
		properties = @Setting(name = STATEMENT_BATCH_SIZE, value = "5"))
@SkipForDialect(dialectClass = MariaDBDialect.class)
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
public class StaleSetTest {
	@Test void test(EntityManagerFactoryScope scope) {
		var entity = new StaleSetTestEntity();
		entity.stringSet.add( "hello" );
		entity.stringSet.add( "world" );
		scope.inTransaction( session -> {
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			var e = session.find( StaleSetTestEntity.class, entity.id );
			e.stringSet.remove( "world" );
			scope.inTransaction( session2 -> {
				var e2 = session2.find( StaleSetTestEntity.class, entity.id );
				e2.stringSet.remove( "world" );
			} );
		} );
	}
	static @Entity class StaleSetTestEntity {
		@Id
		long id;
		@ElementCollection
		Set<String> stringSet = new HashSet<>();
	}
}
