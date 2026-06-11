/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.PropertiesHelper;
import org.hibernate.boot.orchestration.SessionFactoryBootstrap;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;

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

	private PersistenceUnitDescriptor persistenceUnitDescriptor;
	private Map<String, Object> config;

	protected Map<String, Object> getConfig() {
		final Map<String, Object> config = PropertiesHelper.map( Environment.getProperties() );
		ServiceRegistryUtil.applySettings( config );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, createSchema.toPath() );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, dropSchema.toPath() );
		config.put( JAKARTA_HBM2DDL_SCRIPTS_ACTION, "drop-and-create" );
		return config;
	}

	@BeforeEach
	public void setUp() throws IOException {
		createSchema = File.createTempFile( "create_schema", ".sql" );
		dropSchema = File.createTempFile( "drop_schema", ".sql" );
		createSchema.deleteOnExit();
		dropSchema.deleteOnExit();

		persistenceUnitDescriptor = new EntityManagerFactoryBasedFunctionalTest.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ) {
			@Override
			public List<String> getManagedClassNames() {
				return List.of( TestEntity.class.getName() );
			}
		};
		config = getConfig();
	}

	@Test
	@JiraKey(value = "HHH-10972")
	public void testEncoding() throws Exception {

		SessionFactoryBootstrap.generateSchema( persistenceUnitDescriptor, config );

		final String fileContent = new String( Files.readAllBytes( createSchema.toPath() ) )
				.toLowerCase();
		assertTrue( fileContent.contains( expectedTableName() ), () -> scriptContentMessage( fileContent ) );
		assertTrue( fileContent.contains( expectedFieldName() ), () -> scriptContentMessage( fileContent ) );

		final String dropFileContent = new String( Files.readAllBytes(
				dropSchema.toPath() ) ).toLowerCase();
		assertTrue( dropFileContent.contains( expectedTableName() ), () -> scriptContentMessage( dropFileContent ) );
	}

	private static String scriptContentMessage(String content) {
		return "script content (" + content.length() + "): [" + content + "]";
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
