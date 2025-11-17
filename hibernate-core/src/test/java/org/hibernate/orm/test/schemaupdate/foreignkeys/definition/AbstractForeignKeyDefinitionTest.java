/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys.definition;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.EnumSet;

/**
 * @author Vlad MIhalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
public abstract class AbstractForeignKeyDefinitionTest {
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp(ServiceRegistryScope registryScope) {
		final MetadataSources metadataSources = new MetadataSources( registryScope.getRegistry() );

		for ( Class<?> c : getAnnotatedClasses() ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	protected abstract Class<?>[] getAnnotatedClasses();

	protected abstract boolean validate(String fileContent);

	@Test
	@JiraKey(value = "HHH-10643")
	public void testForeignKeyDefinitionOverridesDefaultNamingStrategy(@TempDir File tmpDir) throws Exception {
		final var scriptFile = new File( tmpDir, "script.sql" );

		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( scriptFile.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		String fileContent = new String( Files.readAllBytes( scriptFile.toPath() ) );
		Assertions.assertTrue( validate( fileContent ), "Script file : " + fileContent );
	}

}
