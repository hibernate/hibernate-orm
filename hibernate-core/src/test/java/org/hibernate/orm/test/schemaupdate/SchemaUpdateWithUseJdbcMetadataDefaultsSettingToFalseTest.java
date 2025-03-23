/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13788")
public class SchemaUpdateWithUseJdbcMetadataDefaultsSettingToFalseTest {

	private File updateOutputFile;
	private File createOutputFile;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	public void setUp(String jdbcMetadataExtractorStrategy) throws IOException {
		createOutputFile = File.createTempFile( "create_script", ".sql" );
		createOutputFile.deleteOnExit();
		updateOutputFile = File.createTempFile( "update_script", ".sql" );
		updateOutputFile.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.temp.use_jdbc_metadata_defaults", "false" )
				.applySetting( AvailableSettings.SHOW_SQL, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						jdbcMetadataExtractorStrategy
				)
				.build();

		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( TestEntity.class );

		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
	}

	@AfterEach
	public void tearDown() {
		new SchemaExport().setHaltOnError( true )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@ParameterizedTest
	@EnumSource(JdbcMetadaAccessStrategy.class)
	public void testSchemaUpdateDoesNotTryToRecreateExistingTables(JdbcMetadaAccessStrategy strategy)
			throws Exception {
		setUp( strategy.toString() );

		createSchema();

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( updateOutputFile.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );

		checkNoUpdateStatementHasBeenGenerated();
	}

	private void checkNoUpdateStatementHasBeenGenerated() throws IOException {
		final String fileContent = new String( Files.readAllBytes( updateOutputFile.toPath() ) );
		assertThat(
				"The update output file should be empty because the db schema had already been generated and the domain model was not modified",
				fileContent,
				is( "" )
		);
	}

	private void createSchema() throws Exception {
		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( createOutputFile.getAbsolutePath() )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		new SchemaValidator().validate( metadata );
		checkSchemaHasBeenGenerated();
	}

	private void checkSchemaHasBeenGenerated() throws Exception {
		String fileContent = new String( Files.readAllBytes( createOutputFile.toPath() ) );
		Pattern fileContentPattern = Pattern.compile( "create( (column|row))? table my_test_entity" );
		Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
		assertThat(
				"The schema has not been correctly generated, Script file : " + fileContent.toLowerCase(),
				fileContentMatcher.find(),
				is( true )
		);
	}

	@Entity(name = "My_Test_Entity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;
	}
}
