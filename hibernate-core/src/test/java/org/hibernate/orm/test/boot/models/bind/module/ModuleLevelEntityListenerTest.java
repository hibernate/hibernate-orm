/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module;

import java.nio.file.Path;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-20802")
public class ModuleLevelEntityListenerTest {

	private static final String MODULE_NAME = "test.module.listener";
	private static final String SUBJECT_PACKAGE = "org.hibernate.orm.test.boot.models.bind.module.subject";

	@TempDir
	static Path tempDir;

	static SessionFactory sessionFactory;
	static Class<?> listenerEntityClass;
	static Class<?> excludingEntityClass;
	static Class<?> eventsClass;

	@BeforeAll
	static void setUp() throws Exception {
		final var moduleInfoSource = """
				@jakarta.persistence.EntityListeners(%s.ModuleListener.class)
				module %s {
					requires static jakarta.persistence;
					exports %s;
					opens %s;
				}
				""".formatted( SUBJECT_PACKAGE, MODULE_NAME, SUBJECT_PACKAGE, SUBJECT_PACKAGE );

		final var loaded = TestModuleUtil.compileAndLoadModule(
				tempDir,
				MODULE_NAME,
				moduleInfoSource,
				List.of( SUBJECT_PACKAGE )
		);

		listenerEntityClass = loaded.classLoader().loadClass( SUBJECT_PACKAGE + ".ListenerEntity" );
		excludingEntityClass = loaded.classLoader().loadClass( SUBJECT_PACKAGE + ".ExcludingEntity" );
		eventsClass = loaded.classLoader().loadClass( SUBJECT_PACKAGE + ".EventTracker" );

		final var bootstrapRegistry = new BootstrapServiceRegistryBuilder()
				.applyClassLoader( loaded.classLoader() )
				.build();
		final var serviceRegistry = new StandardServiceRegistryBuilder( bootstrapRegistry )
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
				.build();

		final var metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addAnnotatedClass( listenerEntityClass );
		metadataSources.addAnnotatedClass( excludingEntityClass );
		metadataSources.addModule( loaded.module() );

		sessionFactory = metadataSources.buildMetadata().buildSessionFactory();
	}

	@AfterAll
	static void tearDown() {
		if ( sessionFactory != null ) {
			sessionFactory.close();
		}
	}

	@BeforeEach
	void resetEvents() throws Exception {
		eventsClass.getMethod( "reset" ).invoke( null );
	}

	@AfterEach
	void dropData() {
		sessionFactory.getSchemaManager().truncate();
	}

	@Test
	void callbackOrderingIsModuleThenPackageThenEntityListenerThenEntityCallback() throws Exception {
		sessionFactory.inTransaction( session -> {
			try {
				session.persist( listenerEntityClass.getConstructor( Long.class ).newInstance( 1L ) );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );

		@SuppressWarnings("unchecked")
		var events = (List<String>) eventsClass.getField( "events" ).get( null );
		assertThat( events ).containsExactly(
				"module:ListenerEntity",
				"package:ListenerEntity",
				"entity-listener:ListenerEntity",
				"entity-callback:ListenerEntity"
		);
	}

	@Test
	void excludeDefaultListenersDoesNotExcludeModuleOrPackageLevelEntityListeners() throws Exception {
		sessionFactory.inTransaction( session -> {
			try {
				session.persist( excludingEntityClass.getConstructor( Long.class ).newInstance( 1L ) );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		} );

		@SuppressWarnings("unchecked")
		var events = (List<String>) eventsClass.getField( "events" ).get( null );
		assertThat( events ).containsExactly(
				"module:ExcludingEntity",
				"package:ExcludingEntity",
				"entity-callback:ExcludingEntity"
		);
	}
}
