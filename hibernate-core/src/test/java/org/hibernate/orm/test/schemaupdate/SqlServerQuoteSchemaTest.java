/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-12106" )
@RequiresDialect( SQLServerDialect.class )
@ServiceRegistry(settings = @Setting(name = GLOBALLY_QUOTED_IDENTIFIERS, value = "true"))
@DomainModel
@SessionFactory(exportSchema = false)
public class SqlServerQuoteSchemaTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement()) {
				try {
					statement.execute( "DROP TABLE [my-schema].my_entity" );
				}
				catch (SQLException ignore) {
				}
				try {
					statement.execute( "DROP SCHEMA [my-schema]" );
				}
				catch (SQLException ignore) {
				}
				statement.execute( "CREATE SCHEMA [my-schema]" );
			}
		} ) );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> session.doWork( (connection) -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "DROP TABLE [my-schema].my_entity" );
				statement.execute( "DROP SCHEMA [my-schema]" );
			}
			catch (SQLException ignore) {
			}
		} ) );
	}

	@Test
	public void test(ServiceRegistryScope registryScope, @TempDir File tmpDir) {
		var output = new File( tmpDir, "update_script.sql" );

		// first, export the schema...
		var model = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( MyEntity.class )
				.buildMetadata();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), model );
		try {
			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			Pattern fileContentPattern = Pattern.compile( "create table \\[my\\-schema\\]\\.\\[my_entity\\]" );
			Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
			MatcherAssert.assertThat( fileContentMatcher.find(), is( true ) );
		}
		catch (IOException e) {
			Assertions.fail( e.getMessage() );
		}

		// then, update the schema...
		model = new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClass( MyEntityUpdated.class )
				.buildMetadata();

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setDelimiter( ";" )
				.setFormat( true )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), model );

		try {
			String fileContent = new String( Files.readAllBytes( output.toPath() ) );
			Pattern fileContentPattern = Pattern.compile( "alter table \\[my\\-schema\\]\\.\\[my_entity\\]" );
			Matcher fileContentMatcher = fileContentPattern.matcher( fileContent.toLowerCase() );
			MatcherAssert.assertThat( fileContentMatcher.find(), is( true ) );
		}
		catch (IOException e) {
			Assertions.fail( e.getMessage() );
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
