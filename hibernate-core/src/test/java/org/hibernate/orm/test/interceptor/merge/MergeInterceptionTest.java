/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interceptor.merge;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.Hibernate;
import org.hibernate.cfg.SessionEventSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(annotatedClasses = MergeInterceptionTest.Thing.class,
	integrationSettings = @Setting(name = SessionEventSettings.INTERCEPTOR,
			value = "org.hibernate.orm.test.interceptor.merge.MergeInterceptor"))
class MergeInterceptionTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		Thing t = scope.fromTransaction( em -> {
			Thing thing = new Thing();
			thing.name = "Hibernate";
			assertNull( thing.names );
			em.persist( thing );
			assertNotNull( thing.names );
			assertTrue( Hibernate.isInitialized( thing.names ) );
			assertEquals( 0, thing.names.size() );
			thing.names.add( "CDI" );
			thing.names.add( "Ceylon" );
			return thing;
		} );
		scope.inTransaction( em -> {
			t.name = "Hibernate ORM";
			t.names = null;
			Thing thing = em.merge( t );
			assertNull( t.names );
			assertNotNull( thing.names );
			assertFalse( Hibernate.isInitialized( thing.names ) );
			assertEquals( 2, thing.names.size() );
		} );
	}
	@Entity
	static class Thing {
		@Id @GeneratedValue
		private Long id;
		private String name;
		@ElementCollection
		Set<String> names;
	}
}
