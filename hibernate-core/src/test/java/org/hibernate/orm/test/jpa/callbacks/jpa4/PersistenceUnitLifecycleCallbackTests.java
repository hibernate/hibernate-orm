/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityListener;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.PostCreate;
import jakarta.persistence.PreClose;

import org.hibernate.boot.MetadataSources;
import org.hibernate.models.ModelsException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = {
		PersistenceUnitLifecycleCallbackTests.BasicEntity.class,
		PersistenceUnitLifecycleCallbackTests.PersistenceUnitListener.class
})
@SessionFactory
public class PersistenceUnitLifecycleCallbackTests {
	@Test
	void persistenceUnitLifecycleCallbacksFireForFactoryManagerAndAgent(SessionFactoryScope scope) {
		Events.reset();

		final var factory = scope.getSessionFactory();

		assertThat( Events.names ).containsExactly( "post-create:factory:true" );

		Events.reset();
		final EntityManager entityManager = factory.createEntityManager();

		assertThat( Events.names ).containsExactly( "post-create:manager:true" );

		entityManager.close();

		assertThat( Events.names ).containsExactly(
				"post-create:manager:true",
				"pre-close:manager:true"
		);

		Events.reset();
		final EntityAgent entityAgent = factory.createEntityAgent();

		assertThat( Events.names ).containsExactly( "post-create:agent:true" );

		entityAgent.close();

		assertThat( Events.names ).containsExactly(
				"post-create:agent:true",
				"pre-close:agent:true"
		);

		Events.reset();
		scope.releaseSessionFactory();

		assertThat( Events.names ).containsExactly( "pre-close:factory:true" );
	}

	@Test
	void persistenceUnitLifecycleCallbackSignaturesAreValidated(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( BasicEntity.class )
				.addAnnotatedClass( InvalidPersistenceUnitListener.class )
				.buildMetadata() )
				.isInstanceOf( ModelsException.class )
				.hasMessageContaining( "must return void and have one parameter" )
				.hasMessageContaining( EntityManagerFactory.class.getSimpleName() )
				.hasMessageContaining( EntityManager.class.getSimpleName() )
				.hasMessageContaining( EntityAgent.class.getSimpleName() );
	}

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}
	}

	@Entity(name = "PersistenceUnitLifecycleBasicEntity")
	public static class BasicEntity {
		@Id
		private Integer id;
	}

	@EntityListener
	public static class PersistenceUnitListener {
		@PostCreate
		public void factoryCreated(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "post-create:factory:" + entityManagerFactory.isOpen() );
		}

		@PreClose
		public void factoryClosing(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "pre-close:factory:" + entityManagerFactory.isOpen() );
		}

		@PostCreate
		public void managerCreated(EntityManager entityManager) {
			Events.names.add( "post-create:manager:" + entityManager.isOpen() );
		}

		@PreClose
		public void managerClosing(EntityManager entityManager) {
			Events.names.add( "pre-close:manager:" + entityManager.isOpen() );
		}

		@PostCreate
		public void agentCreated(EntityAgent entityAgent) {
			Events.names.add( "post-create:agent:" + entityAgent.isOpen() );
		}

		@PreClose
		public void agentClosing(EntityAgent entityAgent) {
			Events.names.add( "pre-close:agent:" + entityAgent.isOpen() );
		}
	}

	@EntityListener
	public static class InvalidPersistenceUnitListener {
		@PostCreate
		public void invalid(Object ignored) {
		}
	}
}
