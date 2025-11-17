/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.annotations.Check;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@BaseUnitTest
@JiraKey("HHH-16895")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsColumnCheck.class)
public class CheckConstraintsTest {
	static final String CONSTRAINTS = "my_value <> null";

	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws IOException {
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistry();
	}

	@AfterEach
	public void tearsDown() {
		output.delete();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsTableCheck.class)
	public void testCheckConstraintsAtEntityLevelAreApplied() throws Exception {
		createSchema( EntityLevelCheck.class );
		String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
		assertThat( fileContent ).contains( CONSTRAINTS );
	}

	@Test
	public void testCheckConstraintsAtFieldLevelAreApplied() throws Exception {
		createSchema( FieldLevelCheck.class );
		String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
		assertThat( fileContent ).contains( CONSTRAINTS );
	}

	private void createSchema(Class... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class c : annotatedClasses ) {
			metadataSources.addAnnotatedClass( c );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );
	}


	@Entity(name = "EntityLevelCheck")
	@Check(constraints = CONSTRAINTS)
	public static class EntityLevelCheck {

		@Id
		private Long id;

		@Column(name = "my_value")
		private String myValue;
	}

	@Entity(name = "FieldLevelCheck")
	public static class FieldLevelCheck {

		@Id
		private Long id;

		@Column(name = "my_value")
		@Check(constraints = CONSTRAINTS)
		private String myValue;

	}
}
