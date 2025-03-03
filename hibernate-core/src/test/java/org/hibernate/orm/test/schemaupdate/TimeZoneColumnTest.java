/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetTime;
import java.util.EnumSet;
import java.util.Locale;

import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertTrue;

@BaseUnitTest
@JiraKey("HHH-17448")
public class TimeZoneColumnTest {

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
	public void testTableCommentAreCreated() throws Exception {
		createSchema( TestEntity.class );
		assertTrue(
				tableCreationStatementContainsOptions( output, "birthtime_offset_offset", "option_1" ),
				"TimeZoneColumn options have not been created "
		);
		JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );
		Dialect dialect = jdbcEnvironment.getDialect();
		if ( dialect.supportsCommentOn() ) {
			assertTrue(
					tableCreationStatementContainsComment( output, "birthtime_offset_offset", "This is a comment" ),
					"TimeZoneColumn comment have not been created "
			);
		}
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
				.createOnly( EnumSet.of( TargetType.SCRIPT ), metadata );
	}

	private static boolean tableCreationStatementContainsOptions(
			File output,
			String columnName,
			String options) throws Exception {
		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		for ( int i = 0; i < fileContent.length; i++ ) {
			String statement = fileContent[i].toUpperCase( Locale.ROOT );
			if ( statement.contains( options.toUpperCase( Locale.ROOT ) ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean tableCreationStatementContainsComment(
			File output,
			String columnName,
			String comment) throws Exception {

		String[] fileContent = new String( Files.readAllBytes( output.toPath() ) ).toLowerCase()
				.split( System.lineSeparator() );
		for ( int i = 0; i < fileContent.length; i++ ) {
			String statement = fileContent[i].toUpperCase( Locale.ROOT );
			if ( statement.contains( comment.toUpperCase( Locale.ROOT ) ) ) {
				return true;
			}
		}
		return false;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Long id;

		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "birthtime_offset_offset", comment = "This is a comment", options = "option_1")
		@Column(name = "birthtime_offset")
		private OffsetTime offsetTimeColumn;

//		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
//		@TimeZoneColumn(name = "birthday_zoned_offset")
//		@Column(name = "birthday_zoned")
//		private ZonedDateTime zonedDateTimeColumn;

	}

}
