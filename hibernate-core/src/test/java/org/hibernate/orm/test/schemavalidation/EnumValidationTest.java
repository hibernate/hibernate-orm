/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.ExceptionHandlerLoggedImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Types;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY;

/**
 * Test that an existing tinyint column works even if we switch to smallint type code for enums.
 */
@JiraKey(value = "HHH-15288")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("extractorStrategies")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = EnumValidationTest.TestEntityOld.class)
public class EnumValidationTest implements ServiceRegistryProducer {
	static List<JdbcMetadataAccessStrategy> extractorStrategies() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	private final JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy;

	public EnumValidationTest(JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy) {
		this.jdbcMetadataExtractorStrategy = jdbcMetadataExtractorStrategy;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
	}

	@BeforeEach
	void setUp(DomainModelScope modelScope) {
		final var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		dropSchema( model );
		createSchema( model );
	}

	private void createSchema(MetadataImplementor model) {
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), model );
	}

	@AfterEach
	void tearDown(DomainModelScope modelScope) {
		final var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();

		dropSchema( model );
	}

	private void dropSchema(MetadataImplementor model) {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), model );
	}

	@Test
	public void testValidation(ServiceRegistryScope registryScope) {
		final var newModel = (MetadataImplementor) new MetadataSources( registryScope.getRegistry() )
				.addAnnotatedClasses( TestEntity.class )
				.buildMetadata();
		newModel.orderColumns( false );
		newModel.validate();

		final var tool = registryScope.getRegistry().requireService( SchemaManagementTool.class );

		final var execOptions = new ExecutionOptions() {
			final Map<String, Object> settings = registryScope.getRegistry().requireService( ConfigurationService.class ).getSettings();

			@Override
			public Map<String, Object> getConfigurationValues() {
				return settings;
			}

			@Override
			public boolean shouldManageNamespaces() {
				return false;
			}

			@Override
			public ExceptionHandler getExceptionHandler() {
				return ExceptionHandlerLoggedImpl.INSTANCE;
			}
		};

		tool.getSchemaValidator( null ).doValidation( newModel, execOptions, ContributableMatcher.ALL );
	}

	@SuppressWarnings("unused")
	@Entity(name = "TestEntity")
	public static class TestEntityOld {
		@Id
		public Integer id;

		@Enumerated(EnumType.ORDINAL)
		@Column(name = "enumVal")
		@JdbcTypeCode(Types.TINYINT)
		TestEnum enumVal;
	}

	@SuppressWarnings("unused")
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
}
