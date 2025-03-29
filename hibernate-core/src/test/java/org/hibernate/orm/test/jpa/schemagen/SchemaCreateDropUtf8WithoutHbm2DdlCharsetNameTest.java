/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class SchemaCreateDropUtf8WithoutHbm2DdlCharsetNameTest {

	private File createSchema;
	private File dropSchema;

	private EntityManagerFactoryBuilder entityManagerFactoryBuilder;

	protected Map getConfig() {
		final Map<Object, Object> config = Environment.getProperties();
		ServiceRegistryUtil.applySettings( config );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		ArrayList<Class> classes = new ArrayList<Class>();

		classes.addAll( Arrays.asList( new Class[] {TestEntity.class} ) );
		config.put( AvailableSettings.LOADED_CLASSES, classes );
		return config;
	}

	@BeforeEach
	public void setUp() throws IOException {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		createSchema.deleteOnExit();
		dropSchema.deleteOnExit();

		entityManagerFactoryBuilder = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
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
	@JiraKey(value = "HHH-10972")
	public void testEncoding() throws Exception {

		entityManagerFactoryBuilder.generateSchema();

		final String fileContent = new String( Files.readAllBytes( createSchema.toPath() ) )
				.toLowerCase();
		assertTrue( fileContent.contains( expectedTableName() ) );
		assertTrue( fileContent.contains( expectedFieldName() ) );

		final String dropFileContent = new String( Files.readAllBytes(
				dropSchema.toPath() ) ).toLowerCase();
		assertTrue( dropFileContent.contains( expectedTableName() ) );
	}

	protected String expectedTableName() {
		return "test_" + (char) 233 + "ntity";
	}

	protected String expectedFieldName() {
		return "fi" + (char) 233 + "ld";
	}

	@Entity
	@Table(name = "test_" + (char) 233 +"ntity")
	public static class TestEntity {

		@Id
		@Column(name = "fi" + (char) 233 + "ld")
		private String field;
	}
}
