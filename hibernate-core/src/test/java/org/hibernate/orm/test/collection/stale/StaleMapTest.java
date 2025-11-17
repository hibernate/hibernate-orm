/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.stale;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;

@Jpa(annotatedClasses = StaleMapTest.StaleMapTestEntity.class,
		properties = @Setting(name = STATEMENT_BATCH_SIZE, value = "5"))
@SkipForDialect(dialectClass = MariaDBDialect.class)
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
public class StaleMapTest {
	@Test void test1(EntityManagerFactoryScope scope) {
		var entity = new StaleMapTestEntity();
		entity.intStringMap.put( 0, "hello" );
		entity.intStringMap.put( 1, "world" );
		scope.inTransaction( session -> {
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			var e = session.find( StaleMapTestEntity.class, entity.id );
			e.intStringMap.put( 0, "goodbye" );
			scope.inTransaction( session2 -> {
				var e2 = session2.find( StaleMapTestEntity.class, entity.id );
				e2.intStringMap.put( 0, "BYE" );
			} );
		} );
	}
	@Test void test2(EntityManagerFactoryScope scope) {
		var entity = new StaleMapTestEntity();
		entity.intStringMap.put( 0, "hello" );
		entity.intStringMap.put( 1, "world" );
		scope.inTransaction( session -> {
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			var e = session.find( StaleMapTestEntity.class, entity.id );
			e.intStringMap.put( 1, "everyone" );
			scope.inTransaction( session2 -> {
				var e2 = session2.find( StaleMapTestEntity.class, entity.id );
				e2.intStringMap.remove( 1 );
			} );
		} );
	}
	static @Entity class StaleMapTestEntity {
		@GeneratedValue @Id
		long id;
		@ElementCollection
		Map<Integer,String> intStringMap = new HashMap<>();
	}
}
