/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jira("https://hibernate.atlassian.net/browse/HHH-20802")
public class ModuleLevelAnnotationsTest {

	private static final String MODULE_NAME = "test.module.subject";

	@Entity(name = "ModuleEntity")
	public static class ModuleEntity {
		@Id
		public Long id;
	}

	@Test
	void testNamedQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		final var module = compileAndLoadModule(
				tempDir,
				"""
				@jakarta.persistence.NamedQuery(name = "moduleQuery", query = "select e from ModuleEntity e")
				module test.module.subject {
					requires static jakarta.persistence;
				}
				""",
				null
		);

		final var serviceRegistry = new StandardServiceRegistryBuilder().build();
		try {
			final var metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addAnnotatedClass( ModuleEntity.class );
			metadataSources.addModule( module );

			final var metadata = metadataSources.buildMetadata();
			assertNotNull(
					metadata.getNamedHqlQueryMapping( "moduleQuery" ),
					"Named query 'moduleQuery' defined on module-info.java should be available"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	void testNamedNativeQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		final var module = compileAndLoadModule(
				tempDir,
				"""
				@jakarta.persistence.NamedNativeQuery(name = "moduleNativeQuery", query = "SELECT 1")
				module test.module.subject {
					requires static jakarta.persistence;
				}
				""",
				null
		);

		final var serviceRegistry = new StandardServiceRegistryBuilder().build();
		try {
			final var metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addAnnotatedClass( ModuleEntity.class );
			metadataSources.addModule( module );

			final var metadata = metadataSources.buildMetadata();
			assertNotNull(
					metadata.getNamedNativeQueryMapping( "moduleNativeQuery" ),
					"Named native query 'moduleNativeQuery' defined on module-info.java should be available"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	private Module compileAndLoadModule(
			Path tempDir,
			String moduleInfoSource,
			String extraClasspath) throws Exception {
		final Path srcDir = tempDir.resolve( "src" );
		final Path outDir = tempDir.resolve( "out" );
		final Path moduleSourceDir = srcDir.resolve( MODULE_NAME );
		Files.createDirectories( moduleSourceDir );

		Files.writeString( moduleSourceDir.resolve( "module-info.java" ), moduleInfoSource );

		final Path jakartaPersistenceJar = Path.of(
				jakarta.persistence.NamedQuery.class.getProtectionDomain()
						.getCodeSource().getLocation().toURI()
		);

		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		assertNotNull( compiler );

		var args = new ArrayList<String>();
		args.addAll( List.of(
				"--module-source-path", srcDir.toString(),
				"-d", outDir.toString(),
				"--module-path", jakartaPersistenceJar.toString()
		) );
		if ( extraClasspath != null ) {
			args.addAll( List.of(
					"--add-reads", MODULE_NAME + "=ALL-UNNAMED",
					"-classpath", extraClasspath
			) );
		}
		args.add( moduleSourceDir.resolve( "module-info.java" ).toString() );

		final int result = compiler.run( null, null, null, args.toArray( new String[0] ) );
		assertNotNull( result == 0 ? "" : null, "Compilation of test module failed" );

		final ClassLoader parentLoader = getClass().getClassLoader();
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
		final Module module = moduleLayer.findModule( MODULE_NAME ).orElseThrow();
		controller.addReads( module, parentLoader.getUnnamedModule() );

		return module;
	}
}
