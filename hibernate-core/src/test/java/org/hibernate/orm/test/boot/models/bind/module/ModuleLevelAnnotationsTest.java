/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.module;

import java.nio.file.Path;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataBuilderImplementor;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jira("https://hibernate.atlassian.net/browse/HHH-20802")
public class ModuleLevelAnnotationsTest {

	private static final String MODULE_NAME = "test.module.subject";

	private static final String SUBJECT_PACKAGE = "org.hibernate.orm.test.boot.models.bind.module.subject";

	@Entity(name = "ModuleEntity")
	public static class ModuleEntity {
		@Id
		public Long id;
	}

	@Test
	void testJpaNamedQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@jakarta.persistence.NamedQuery(name = \"moduleQuery\", query = \"select e from ModuleEntity e\")",
				metadata -> assertNotNull(
						metadata.getNamedHqlQueryMapping( "moduleQuery" ),
						"Named query 'moduleQuery' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testJpaNamedNativeQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@jakarta.persistence.NamedNativeQuery(name = \"moduleNativeQuery\", query = \"SELECT 1\")",
				metadata -> assertNotNull(
						metadata.getNamedNativeQueryMapping( "moduleNativeQuery" ),
						"Named native query 'moduleNativeQuery' defined on module-info.java should be available"
				)
		);
	}

	@Test
	void testJpaNamedStoredProcedureQueryOnModuleInfo(@TempDir Path tempDir) throws Exception {
		assertModuleAnnotation(
				tempDir,
				"@jakarta.persistence.NamedStoredProcedureQuery(name = \"moduleProc\", procedureName = \"my_procedure\")",
				metadata -> assertNotNull(
						metadata.getNamedProcedureCallMapping( "moduleProc" ),
						"Named stored procedure query 'moduleProc' defined on module-info.java should be available"
				)
		);
	}

	private void assertModuleAnnotation(
			Path tempDir,
			String annotation,
			MetadataAssertion assertion) throws Exception {
		assertModuleAnnotation( tempDir, annotation, List.of(), (metadata, loaded) -> assertion.verify( metadata ) );
	}

	private void assertModuleAnnotation(
			Path tempDir,
			String annotation,
			List<String> classPackages,
			ModuleMetadataAssertion assertion) throws Exception {
		assertModuleAnnotation( tempDir, annotation, classPackages, List.of(), assertion );
	}

	private void assertModuleAnnotation(
			Path tempDir,
			String annotation,
			List<String> classPackages,
			List<String> moduleEntitySimpleNames,
			ModuleMetadataAssertion assertion) throws Exception {
		final var opens = classPackages.stream()
				.map( pkg -> "\topens " + pkg + ";\n" )
				.reduce( "", String::concat );
		final var moduleInfoSource = """
				%s
				module %s {
					requires static jakarta.persistence;
					requires static org.hibernate.orm.core;
				%s}
				""".formatted( annotation, MODULE_NAME, opens );

		final var loaded = TestModuleUtil.compileAndLoadModule(
				tempDir, MODULE_NAME, moduleInfoSource, classPackages
		);

		final var bootstrapRegistry = new BootstrapServiceRegistryBuilder()
				.applyClassLoader( loaded.classLoader() )
				.build();
		final var serviceRegistry = new StandardServiceRegistryBuilder( bootstrapRegistry ).build();
		try {
			final var metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addAnnotatedClass( ModuleEntity.class );
			for ( String simpleClassName : moduleEntitySimpleNames ) {
				metadataSources.addAnnotatedClass(
						loaded.classLoader().loadClass( SUBJECT_PACKAGE + "." + simpleClassName )
				);
			}
			metadataSources.addModule( loaded.module() );

			// Pre-register the module in the ModuleDetailsRegistry, because
			// resolveModuleDetails(String) only searches ModuleLayer.boot()
			// and cannot find modules from custom module layers.
			final var builder = (MetadataBuilderImplementor) metadataSources.getMetadataBuilder();
			builder.getBootstrapContext().getModelsContext()
					.getModuleDetailsRegistry().resolveModuleDetails( loaded.module() );

			assertion.verify( builder.build(), loaded );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@FunctionalInterface
	interface MetadataAssertion {
		void verify(Metadata metadata);
	}

	@FunctionalInterface
	interface ModuleMetadataAssertion {
		void verify(Metadata metadata, TestModuleUtil.LoadedModule loaded) throws Exception;
	}
}
