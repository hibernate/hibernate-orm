/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@JiraKey("HHH-16762")
public class LobSchemaUpdateTest {
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
		dropDatabase( TestEntity.class );
		output.delete();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testUpdateIsNotExecuted() throws Exception {
		createSchema( TestEntity.class );
		updateSchema( TestEntity.class );
		String fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.replace( System.lineSeparator(), "" );
		assertThat( fileContent ).isEmpty();
	}

	private void createSchema(Class<?>... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class<?> annotatedClass : annotatedClasses ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( false )
				.setFormat( false )
				.create( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private void updateSchema(Class<?>... annotatedClasses) {
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class<?> annotatedClass : annotatedClasses ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.SCRIPT, TargetType.DATABASE ), metadata );
	}

	private void dropDatabase(Class<?>... annotatedClasses){
		final MetadataSources metadataSources = new MetadataSources( ssr );

		for ( Class<?> annotatedClass : annotatedClasses ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		new SchemaExport()
				.setHaltOnError( false )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "TestEntity")
	public class TestEntity {

		@Id
		@GeneratedValue
		protected int id;

		@Lob
		protected String clobField;

		@Lob
		protected byte[] blobField;

		String name;

		public TestEntity() {}

	}

}
