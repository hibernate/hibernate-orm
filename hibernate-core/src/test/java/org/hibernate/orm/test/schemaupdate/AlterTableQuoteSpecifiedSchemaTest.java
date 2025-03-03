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
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Guillaume Smet
 */
@JiraKey(value = "HHH-12939")
@RequiresDialect(value = H2Dialect.class)
@RequiresDialect(value = PostgreSQLDialect.class)
@RequiresDialect(value = SQLServerDialect.class, majorVersion = 11)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
public class AlterTableQuoteSpecifiedSchemaTest extends AbstractAlterTableQuoteSchemaTest {

	@BeforeEach
	protected void init() {
		try {
			inTransaction(
					session -> session.createNativeQuery( "DROP TABLE " + quote( "my-schema", "my_entity" ) )
							.executeUpdate()
			);
		}
		catch (Exception e) {
		}
		try {
			inTransaction(
					session -> session.createNativeQuery( "DROP SCHEMA " + quote( "my-schema" ) )
							.executeUpdate()
			);
		}
		catch (Exception e) {
		}
		try {
			inTransaction(
					session -> session.createNativeQuery( "CREATE SCHEMA " + quote( "my-schema" ) )
							.executeUpdate()
			);
		}
		catch (Exception e) {
		}
	}

	@AfterEach
	protected void cleanupTestData() {
		try {
			inTransaction(
					session -> session.createNativeQuery( "DROP SCHEMA " + quote( "my-schema" ) )
							.executeUpdate()
			);
		}
		catch (Exception e) {
		}
	}

	@Test
	public void testSpecifiedSchema() throws IOException {
		File output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();

		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString() )
				.build();

		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( MyEntity.class )
					.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			new SchemaUpdate()
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( ";" )
					.setFormat( true )
					.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

		try {
			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			Pattern fileContentPattern = Pattern.compile( "create table " + regexpQuote( "my-schema", "my_entity" ) );
			Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
			assertThat( fileContentMatcher.find(), is( true ) );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}

		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, Boolean.TRUE.toString() )
				.build();
		try {
			final MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( MyEntityUpdated.class )
					.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			new SchemaUpdate()
					.setHaltOnError( true )
					.setOutputFile( output.getAbsolutePath() )
					.setDelimiter( ";" )
					.setFormat( true )
					.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}

		try {
			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			Pattern fileContentPattern = Pattern.compile( "alter table.* " + regexpQuote( "my-schema", "my_entity" ) );
			Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
			assertThat( fileContentMatcher.find(), is( true ) );
		}
		catch (IOException e) {
			fail( e.getMessage() );
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity", schema = "my-schema")
	public static class MyEntity {

		@Id
		public Integer id;
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity", schema = "my-schema")
	public static class MyEntityUpdated {

		@Id
		public Integer id;

		private String title;
	}
}
