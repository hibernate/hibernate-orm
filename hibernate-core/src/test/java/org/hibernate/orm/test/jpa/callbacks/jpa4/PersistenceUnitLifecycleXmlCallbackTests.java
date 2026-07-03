/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.jpa4;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityAgent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = PersistenceUnitLifecycleXmlCallbackTests.BasicEntity.class,
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/jpa4/persistence-unit-lifecycle-callbacks.orm.xml"
)
@SessionFactory
public class PersistenceUnitLifecycleXmlCallbackTests {
	@Test
	void persistenceUnitLifecycleCallbacksDeclaredInXmlFire(SessionFactoryScope scope) {
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

	public static class Events {
		static final List<String> names = new ArrayList<>();

		static void reset() {
			names.clear();
		}
	}

	@Entity(name = "PersistenceUnitLifecycleXmlBasicEntity")
	public static class BasicEntity {
		@Id
		private Integer id;
	}

	public static class XmlFactoryListener {
		public void factoryCreated(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "post-create:factory:" + entityManagerFactory.isOpen() );
		}

		public void factoryClosing(EntityManagerFactory entityManagerFactory) {
			Events.names.add( "pre-close:factory:" + entityManagerFactory.isOpen() );
		}
	}

	public static class XmlManagerListener {
		public void managerCreated(EntityManager entityManager) {
			Events.names.add( "post-create:manager:" + entityManager.isOpen() );
		}

		public void managerClosing(EntityManager entityManager) {
			Events.names.add( "pre-close:manager:" + entityManager.isOpen() );
		}
	}

	public static class XmlAgentListener {
		public void agentCreated(EntityAgent entityAgent) {
			Events.names.add( "post-create:agent:" + entityAgent.isOpen() );
		}

		public void agentClosing(EntityAgent entityAgent) {
			Events.names.add( "pre-close:agent:" + entityAgent.isOpen() );
		}
	}
}
