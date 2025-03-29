/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@BaseUnitTest
@JiraKey(value = "HHH-15573")
public class SpannerSchemaCreationColumnTypesTest {
	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@BeforeEach
	public void setUp() throws IOException {
		output = File.createTempFile( "create_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.applySetting( AvailableSettings.DIALECT, SpannerDialect.class )
				.build();

		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( TestEntity.class );

		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	@AfterEach
	public void tearDown() {
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaCreation() throws Exception {
		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.create( EnumSet.of( TargetType.SCRIPT ), metadata );

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );

		assertThat(fileContent.toLowerCase( Locale.ROOT )).contains( "create table test_entity_table" );
		assertThat(fileContent.toLowerCase( Locale.ROOT )).contains( "id int64 not null" );
		assertThat(fileContent.toLowerCase( Locale.ROOT )).contains( "name_column string(255)" );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY_TABLE")
	public static class TestEntity {

		@Id
		@Column(name = "ID")
		public Integer id;

		@Column(name = "NAME_COLUMN")
		public String name;
	}
}
