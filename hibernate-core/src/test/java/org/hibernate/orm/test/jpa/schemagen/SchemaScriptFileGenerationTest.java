/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
public class SchemaScriptFileGenerationTest {
	private File createSchema;
	private File dropSchema;
	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	@BeforeEach
	public void setUp() throws IOException {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		createSchema.deleteOnExit();
		dropSchema.deleteOnExit();

		entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				buildPersistenceUnitDescriptor(),
				getConfig()
		);
	}

	@AfterEach
	public void destroy() {
		if ( entityManagerFactoryBuilder != null ) {
			entityManagerFactoryBuilder.cancel();
		}
	}

	@Test
	@JiraKey(value = "10601")
	public void testGenerateSchemaDoesNotProduceTheSameStatementTwice() throws Exception {

		entityManagerFactoryBuilder.generateSchema();

		final String fileContent = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();

		Pattern createStatementPattern = Pattern.compile( "create( (column|row))? table test_entity" );
		Matcher createStatementMatcher = createStatementPattern.matcher( fileContent );
		assertThat( createStatementMatcher.find(), is( true ) );
		assertThat(
				"The statement 'create table test_entity' is generated twice",
				createStatementMatcher.find(),
				is( false )
		);

		final String dropFileContent = new String( Files.readAllBytes( dropSchema.toPath() ) ).toLowerCase();
		assertThat( dropFileContent.contains( "drop table " ), is( true ) );
		assertThat(
				"The statement 'drop table ' is generated twice",
				dropFileContent.replaceFirst( "drop table ", "" ).contains( "drop table " ),
				is( false )
		);
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private String field;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}
	}

	private PersistenceUnitDescriptor buildPersistenceUnitDescriptor() {
		return new EntityManagerFactoryBasedFunctionalTest.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() );
	}

	private Map getConfig() {
		final Map<Object, Object> config = Environment.getProperties();
		ServiceRegistryUtil.applySettings( config );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( new Class[] {TestEntity.class} ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}
}
