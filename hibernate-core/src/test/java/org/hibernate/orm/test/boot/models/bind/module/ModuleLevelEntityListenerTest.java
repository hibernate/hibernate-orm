/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jira("https://hibernate.atlassian.net/browse/HHH-20802")
public class ModuleLevelEntityListenerTest {

	private static final String MODULE_NAME = "test.module.listener";

	@TempDir
	static Path tempDir;

	static Module module;
	static ClassLoader moduleClassLoader;
	static Class<?> listenerEntityClass;
	static Class<?> excludingEntityClass;
	static Class<?> eventsClass;
	static SessionFactory sessionFactory;

	@BeforeAll
	static void setUp() throws Exception {
		final Path srcDir = tempDir.resolve( "src" );
		final Path outDir = tempDir.resolve( "out" );

		final Path moduleSourceDir = srcDir.resolve( MODULE_NAME );
		final Path pkgDir = moduleSourceDir.resolve( "test/module/listener" );
		Files.createDirectories( pkgDir );

		Files.writeString(
				moduleSourceDir.resolve( "module-info.java" ),
				"""
				@jakarta.persistence.EntityListeners(test.module.listener.ModuleListener.class)
				module test.module.listener {
					requires static jakarta.persistence;
					exports test.module.listener;
					opens test.module.listener;
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "EventTracker.java" ),
				"""
				package test.module.listener;

				import java.util.ArrayList;
				import java.util.List;

				public class EventTracker {
					public static final List<String> events = new ArrayList<>();

					public static void reset() {
						events.clear();
					}
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "ModuleListener.java" ),
				"""
				package test.module.listener;

				import jakarta.persistence.PrePersist;

				public class ModuleListener {
					@PrePersist
					public void prePersist(Object entity) {
						EventTracker.events.add( "module:" + entity.getClass().getSimpleName() );
					}
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "EntityLevelListener.java" ),
				"""
				package test.module.listener;

				import jakarta.persistence.PrePersist;

				public class EntityLevelListener {
					@PrePersist
					public void prePersist(Object entity) {
						EventTracker.events.add( "entity-listener:" + entity.getClass().getSimpleName() );
					}
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "ListenerEntity.java" ),
				"""
				package test.module.listener;

				import jakarta.persistence.Entity;
				import jakarta.persistence.EntityListeners;
				import jakarta.persistence.Id;
				import jakarta.persistence.PrePersist;

				@Entity(name = "ListenerEntity")
				@EntityListeners(EntityLevelListener.class)
				public class ListenerEntity {
					@Id
					public Long id;

					public ListenerEntity() {
					}

					public ListenerEntity(Long id) {
						this.id = id;
					}

					@PrePersist
					void prePersist() {
						EventTracker.events.add( "entity-callback:" + getClass().getSimpleName() );
					}
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "ExcludingEntity.java" ),
				"""
				package test.module.listener;

				import jakarta.persistence.Entity;
				import jakarta.persistence.ExcludeDefaultListeners;
				import jakarta.persistence.Id;
				import jakarta.persistence.PrePersist;

				@Entity(name = "ExcludingEntity")
				@ExcludeDefaultListeners
				public class ExcludingEntity {
					@Id
					public Long id;

					public ExcludingEntity() {
					}

					public ExcludingEntity(Long id) {
						this.id = id;
					}

					@PrePersist
					void prePersist() {
						EventTracker.events.add( "entity-callback:" + getClass().getSimpleName() );
					}
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "PackageListener.java" ),
				"""
				package test.module.listener;

				import jakarta.persistence.PrePersist;

				public class PackageListener {
					@PrePersist
					public void prePersist(Object entity) {
						EventTracker.events.add( "package:" + entity.getClass().getSimpleName() );
					}
				}
				"""
		);

		Files.writeString(
				pkgDir.resolve( "package-info.java" ),
				"""
				@jakarta.persistence.EntityListeners(test.module.listener.PackageListener.class)
				package test.module.listener;
				"""
		);

		final Path jakartaPersistenceJar = Path.of(
				jakarta.persistence.NamedQuery.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI()
		);

		Files.createDirectories( outDir );

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull( compiler );
		final int result = compiler.run(
				null, null, null,
				"--module-source-path", srcDir.toString(),
				"-d", outDir.toString(),
				"--module-path", jakartaPersistenceJar.toString(),
				moduleSourceDir.resolve( "module-info.java" ).toString(),
				pkgDir.resolve( "EventTracker.java" ).toString(),
				pkgDir.resolve( "ModuleListener.java" ).toString(),
				pkgDir.resolve( "EntityLevelListener.java" ).toString(),
				pkgDir.resolve( "ListenerEntity.java" ).toString(),
				pkgDir.resolve( "ExcludingEntity.java" ).toString(),
				pkgDir.resolve( "PackageListener.java" ).toString(),
				pkgDir.resolve( "package-info.java" ).toString()
		);
		assertNotNull( result == 0 ? "" : null, "Compilation of test module failed" );

		final ClassLoader parentLoader = ModuleLevelEntityListenerTest.class.getClassLoader();
		final ModuleFinder finder = ModuleFinder.of( outDir );
		final ModuleLayer parentLayer = ModuleLayer.boot();
		final Configuration cfg = parentLayer.configuration().resolve(
				finder,
				ModuleFinder.of(),
				Set.of( MODULE_NAME )
		);
		final ModuleLayer.Controller controller = ModuleLayer.defineModulesWithOneLoader(
				cfg,
				List.of( parentLayer ),
				parentLoader
		);
		final ModuleLayer moduleLayer = controller.layer();
		module = moduleLayer.findModule( MODULE_NAME ).orElseThrow();
		controller.addReads( module, parentLoader.getUnnamedModule() );

		moduleClassLoader = moduleLayer.findLoader( MODULE_NAME );
		listenerEntityClass = moduleClassLoader.loadClass( "test.module.listener.ListenerEntity" );
		excludingEntityClass = moduleClassLoader.loadClass( "test.module.listener.ExcludingEntity" );
		eventsClass = moduleClassLoader.loadClass( "test.module.listener.EventTracker" );

		final var bootstrapRegistry = new BootstrapServiceRegistryBuilder()
				.applyClassLoader( moduleClassLoader )
				.build();
		final var serviceRegistry = new StandardServiceRegistryBuilder( bootstrapRegistry )
				.applySetting( "hibernate.hbm2ddl.auto", "create-drop" )
				.build();

		final var metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addAnnotatedClass( listenerEntityClass );
		metadataSources.addAnnotatedClass( excludingEntityClass );
		metadataSources.addModule( module );

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
