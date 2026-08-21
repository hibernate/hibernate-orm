/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.SourceType;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.ScriptSourceInput;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.SourceDescriptor;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@JiraKey("HHH-17293")
@BaseUnitTest
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(H2Dialect.class)
public class DurationValidationTest implements ExecutionOptions {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	private static Stream<Arguments> jdbcMetadataExtractorStrategies() {
		return Stream.of(
				arguments( JdbcMetadataAccessStrategy.GROUPED.toString() ),
				arguments( JdbcMetadataAccessStrategy.INDIVIDUALLY.toString() )
		);
	}

	public void setUp(String jdbcMetadataExtractorStrategy) {
		try {
			ssr = ServiceRegistryUtil.serviceRegistryBuilder()
					.applySetting(
							AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
							jdbcMetadataExtractorStrategy
					)
					.build();
			metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( TestEntity.class )
					.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();


			dropSchema();
			// create the schema
			createSchema();
		}
		catch (Exception e) {
			tearDown();
			throw e;
		}
	}

	@AfterEach
	public void tearDown() {
		try {
			dropSchema();
		}
		finally {
			if ( ssr != null ) {
				StandardServiceRegistryBuilder.destroy( ssr );
			}
		}

	}

	@ParameterizedTest
	@MethodSource("jdbcMetadataExtractorStrategies")
	public void testValidation(String jdbcMetadataExtractorStrategy) {
		setUp( jdbcMetadataExtractorStrategy );
		doValidation();
	}

	private void doValidation() {
		ssr.getService( SchemaManagementTool.class ).getSchemaValidator( null )
				.doValidation( metadata, this, ContributableMatcher.ALL );
	}

	private void createSchema() {
		ssr.getService( SchemaManagementTool.class ).getSchemaCreator( null ).doCreation(
				metadata,
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
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		@Column(name = "duration_interval", precision = 10, scale = 6)
		@JdbcTypeCode(SqlTypes.INTERVAL_SECOND)
		private Duration durationIntervalSecond;

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
