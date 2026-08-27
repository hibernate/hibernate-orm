/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.integrationtest.java.module.test.annotation.ModuleAnnotationEntity;
import org.hibernate.orm.integrationtest.java.module.test.listener.EventTracker;
import org.hibernate.orm.integrationtest.java.module.test.listener.ExcludingEntity;
import org.hibernate.orm.integrationtest.java.module.test.listener.ListenerEntity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-20802">HHH-20802</a>
 */
public class ModuleLevelEntityListenerIT {

	static SessionFactory sessionFactory;

	@BeforeAll
	static void setUp() {
		var serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" )
				.applySetting( "hibernate.connection.url",
						"jdbc:h2:mem:module_listeners;DB_CLOSE_DELAY=-1" )
				.applySetting( "hibernate.connection.username", "sa" )
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
				.build();

		var metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addAnnotatedClass( ListenerEntity.class );
		metadataSources.addAnnotatedClass( ExcludingEntity.class );
		metadataSources.addAnnotatedClass( ModuleAnnotationEntity.class );
		metadataSources.addModule( ModuleLevelEntityListenerIT.class.getModule() );

		sessionFactory = metadataSources.buildMetadata().buildSessionFactory();
	}

	@AfterAll
	static void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@BeforeEach
	void resetEvents() {
		EventTracker.reset();
	}

	@AfterEach
	void dropData() {
		sessionFactory.getSchemaManager().truncate();
	}

	@Test
	void callbackOrderingIsModuleThenPackageThenEntityListenerThenEntityCallback() {
		sessionFactory.inTransaction( session -> {
			session.persist( new ListenerEntity( 1L ) );
		} );

		assertThat( EventTracker.events ).containsExactly(
				"module:ListenerEntity",
				"package:ListenerEntity",
				"entity-listener:ListenerEntity",
				"entity-callback:ListenerEntity"
		);
	}

	@Test
	void excludeDefaultListenersDoesNotExcludeModuleOrPackageLevelEntityListeners() {
		sessionFactory.inTransaction( session -> {
			session.persist( new ExcludingEntity( 1L ) );
		} );

		assertThat( EventTracker.events ).containsExactly(
				"module:ExcludingEntity",
				"package:ExcludingEntity",
				"entity-callback:ExcludingEntity"
		);
	}
}
