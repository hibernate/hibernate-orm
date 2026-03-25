/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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

		entityManagerFactoryBuilder.generateSchema();
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
		String createFileContent = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();

		Pattern createStatementPattern = Pattern.compile( "create( (column|row))? table test_entity" );
		Matcher createStatementMatcher = createStatementPattern.matcher( createFileContent );
		assertThat(
				"Missing create table: " + createFileContent,
				createStatementMatcher.find(),
				is( true ) );
		assertThat(
				"The statement 'create table test_entity' is generated twice: " + createFileContent,
				createStatementMatcher.find(),
				is( false )
		);

		String dropFileContent = new String( Files.readAllBytes( dropSchema.toPath() ) ).toLowerCase();

		Pattern dropStatementPattern = Pattern.compile( "drop table( if exists)? test_entity" );
		Matcher dropStatementMatcher = dropStatementPattern.matcher( dropFileContent );
		assertThat( "Missing drop table: " + dropFileContent,
				dropStatementMatcher.find(),
				is( true ) );
		assertThat(
				"The statement 'drop table' is generated twice: " + dropFileContent,
				dropStatementMatcher.find(),
				is( false )
		);
	}

	@Test
	@JiraKey(value = "HHH-20289")
	public void testGenerateSchemaNullDistinctUniqueIndex() throws Exception {
		String createFileContent = new String( Files.readAllBytes( createSchema.toPath() ) ).toLowerCase();
		assertThat(
				"Missing create unique index: " + createFileContent,
				createFileContent.contains( "create unique nulls distinct index" ),
				is( true ) );
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private String field;

		@OneToOne
		private TestChildEntity child;

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		public TestChildEntity getChild() {
			return child;
		}

		public void setChild(TestChildEntity child) {
			this.child = child;
		}
	}

	@Entity
	@Table(name = "test_child_entity")
	public static class TestChildEntity {
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
		return new EntityManagerFactoryBasedFunctionalTest.TestingPersistenceUnitDescriptorImpl(
				getClass().getSimpleName() );
	}

	private Map<String, Object> getConfig() {
		final Map<String, Object> config = PropertiesHelper.map( Environment.getProperties() );
		ServiceRegistryUtil.applySettings( config );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		config.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		List<Class<?>> classes = Arrays.asList( TestEntity.class, TestChildEntity.class );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}
}
