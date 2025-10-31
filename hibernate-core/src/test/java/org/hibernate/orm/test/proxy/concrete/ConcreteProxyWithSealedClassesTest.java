/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.proxy.concrete;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the compatibility of Hibernate {@code @ConcreteProxy} with Java {@code sealed}
 * inheritance hierarchies. Proxy generation may fail during {@code SessionFactory}
 * bootstrap if the abstract sealed root cannot be subclassed at runtime. These tests
 * document the limitation and assert the expected failure behavior.
 *
 * @author Vincent Bouthinon
 */
@Jpa(annotatedClasses = {
		ConcreteProxyWithSealedClassesTest.Actor.class,
		ConcreteProxyWithSealedClassesTest.Postman.class,
		ConcreteProxyWithSealedClassesTest.Scene.class
})
@JiraKey("HHH-19899")
class ConcreteProxyWithSealedClassesTest {

	@Test
	void getReference(EntityManagerFactoryScope scope) {
		var id = scope.fromTransaction( entityManager -> {
			Actor actor = new Postman();
			entityManager.persist( actor );
			return actor.id;
		} );
		scope.inTransaction( entityManager -> {
			Actor actor = entityManager.getReference( Actor.class, id );
			assertThat( actor ).isInstanceOf( Postman.class );
		} );
	}

	@Test
	void lazyAssociation(EntityManagerFactoryScope scope) {
		var id = scope.fromTransaction( entityManager -> {
			Actor actor = new Postman();
			entityManager.persist( actor );
			Scene scene = new Scene();
			scene.setActor( actor );
			entityManager.persist( scene );
			return scene.id;
		} );
		scope.inTransaction( entityManager -> {
			Scene scene = entityManager.find( Scene.class, id );
			assertThat( scene.getActor() ).isInstanceOf( Postman.class );
		} );
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
	public static non-sealed class Postman extends Actor {
	}

	@Entity(name = "Scene")
	public static class Scene {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private Actor actor;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Actor getActor() {
			return actor;
		}

		public void setActor(Actor actor) {
			this.actor = actor;
		}
	}
}
