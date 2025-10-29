/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Tests the compatibility of Hibernate {@code @ConcreteProxy} with Java {@code sealed}
 * inheritance hierarchies. Proxy generation may fail during {@code SessionFactory}
 * bootstrap if the abstract sealed root cannot be subclassed at runtime. These tests
 * document the limitation and assert the expected failure behavior.
 *
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {ConcreteProxyWithSealedClassesTest.Actor.class},
		integrationSettings = {
				@Setting(name = org.hibernate.cfg.AvailableSettings.SHOW_SQL, value = "true"),
		}
)
@JiraKey("HHH-19899")
class ConcreteProxyWithSealedClassesTest {

	@Test
	void testConcreteProxyWithSealedClassesTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Actor actor = new Postman();
					entityManager.persist( actor );
					entityManager.flush();
				}
		);
	}

	@Entity(name = "actor")
	@Table(name = "actor")
	@ConcreteProxy
	public static abstract sealed class Actor {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "Postman")
	@Table(name = "Postman")
	@ConcreteProxy
	public static non-sealed class Postman extends Actor {
	}
}
