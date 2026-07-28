/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.GenerationTarget;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Tests that dialects use "DROP SCHEMA IF EXISTS" and "CREATE SCHEMA IF NOT EXISTS"
 * when the database supports it, to avoid warnings/errors when using schema management
 * with custom schemas.
 *
 * @author Yoann Rodière
 */
@JiraKey("HHH-20708")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
public class SchemaDropIfExistsTest {

	private StandardServiceRegistry serviceRegistry;
	private Metadata metadata;

	@BeforeEach
	public void setUp() {
		serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_CREATE_SCHEMAS, true )
				.build();

		metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
	}

	@AfterEach
	public void tearDown() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsDropSchemaIfExists.class)
	public void testDropSchemaUsesIfExists() {
		List<String> commands = collectDropCommands();
		assertThat(
				"Dialect should use 'DROP SCHEMA IF EXISTS' to avoid warnings on non-existent schemas",
				commands,
				hasItem( containsString( "drop schema if exists" ) )
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsDropSchemaIfExists.class, reverse = true)
	public void testDropSchemaDoesNotUseIfExists() {
		List<String> commands = collectDropCommands();
		assertThat(
				"Dialect should not use 'DROP SCHEMA IF EXISTS' when the database doesn't support it",
				commands,
				not( hasItem( containsString( "drop schema if exists" ) ) )
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCreateSchemaIfNotExists.class)
	public void testCreateSchemaUsesIfNotExists() {
		List<String> commands = collectCreateCommands();
		assertThat(
				"Dialect should use 'CREATE SCHEMA IF NOT EXISTS' to avoid errors when schema already exists",
				commands,
				hasItem( containsString( "create schema if not exists" ) )
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCreateSchemaIfNotExists.class, reverse = true)
	public void testCreateSchemaDoesNotUseIfNotExists() {
		List<String> commands = collectCreateCommands();
		assertThat(
				"Dialect should not use 'CREATE SCHEMA IF NOT EXISTS' when the database doesn't support it",
				commands,
				not( hasItem( containsString( "create schema if not exists" ) ) )
		);
	}

	private List<String> collectDropCommands() {
		RecordingTarget target = new RecordingTarget();
		new SchemaDropperImpl( serviceRegistry ).doDrop( metadata, true, target );
		return target.commands;
	}

	private List<String> collectCreateCommands() {
		RecordingTarget target = new RecordingTarget();
		new SchemaCreatorImpl( serviceRegistry ).doCreation( metadata, true, target );
		return target.commands;
	}

	@Entity
	@Table(name = "test_entity", schema = "myschema")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	private static class RecordingTarget implements GenerationTarget {
		private final List<String> commands = new ArrayList<>();

		@Override
		public void prepare() {
		}

		@Override
		public void accept(String command) {
			commands.add( command.toLowerCase() );
		}

		@Override
		public void release() {
		}
	}
}
