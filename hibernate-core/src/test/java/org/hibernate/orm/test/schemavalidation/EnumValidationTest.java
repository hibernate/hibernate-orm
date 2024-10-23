/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

/**
 * Test that an existing tinyint column works even if we switch to smallint type code for enums.
 */
@JiraKey(value = "HHH-15288")
@RunWith(Parameterized.class)
public class EnumValidationTest implements ExecutionOptions {
	@Parameterized.Parameters
	public static Collection<String> parameters() {
		return Arrays.asList(
				JdbcMetadaAccessStrategy.GROUPED.toString(),
				JdbcMetadaAccessStrategy.INDIVIDUALLY.toString()
		);
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;
	private MetadataImplementor oldMetadata;

	@Before
	public void beforeTest() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						jdbcMetadataExtractorStrategy
				)
				.build();
		oldMetadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TestEntityOld.class )
				.buildMetadata();
		oldMetadata.orderColumns( false );
		oldMetadata.validate();
		metadata = (MetadataImplementor) new MetadataSources( ssr )
				.addAnnotatedClass( TestEntity.class )
				.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		try {
			dropSchema();
			// create the schema
			createSchema();
		}
		catch (Exception e) {
			tearDown();
			throw e;
		}
	}

	@After
	public void tearDown() {
		dropSchema();
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testValidation() {
		doValidation();
	}

	private void doValidation() {
		ssr.getService( SchemaManagementTool.class ).getSchemaValidator( null )
				.doValidation( metadata, this, ContributableMatcher.ALL );
	}

	private void createSchema() {
		ssr.getService( SchemaManagementTool.class ).getSchemaCreator( null ).doCreation(
				oldMetadata,
				this,
				ContributableMatcher.ALL,
				new SourceDescriptor() {
					@Override
					public SourceType getSourceType() {
						return SourceType.METADATA;
					}

					@Override
					public ScriptSourceInput getScriptSourceInput() {
						return null;
					}
				},
				new TargetDescriptor() {
					@Override
					public EnumSet<TargetType> getTargetTypes() {
						return EnumSet.of( TargetType.DATABASE );
					}

					@Override
					public ScriptTargetOutput getScriptTargetOutput() {
						return null;
					}
				}
		);
	}

	private void dropSchema() {
		new SchemaExport()
				.drop( EnumSet.of( TargetType.DATABASE ), oldMetadata );
	}

	@Entity(name = "TestEntity")
	public static class TestEntityOld {
		@Id
		public Integer id;

		@Enumerated(EnumType.ORDINAL)
		@Column(name = "enumVal")
		@JdbcTypeCode(Types.TINYINT)
		TestEnum enumVal;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Integer id;

		@Enumerated(EnumType.ORDINAL)
		@Column(name = "enumVal")
		TestEnum enumVal;
	}

	public enum TestEnum {
		VALUE1,
		VALUE2
	}

	@Override
	public Map<String,Object> getConfigurationValues() {
		return ssr.requireService( ConfigurationService.class ).getSettings();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return false;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return ExceptionHandlerLoggedImpl.INSTANCE;
	}
}
