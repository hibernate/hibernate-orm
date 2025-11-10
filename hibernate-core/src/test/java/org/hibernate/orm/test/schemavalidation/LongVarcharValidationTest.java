/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
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
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-9693")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("extractorStrategies")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = LongVarcharValidationTest.Translation.class)
public class LongVarcharValidationTest implements ServiceRegistryProducer {
	static List<JdbcMetadataAccessStrategy> extractorStrategies() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	private final JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy;

	public LongVarcharValidationTest(JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy) {
		this.jdbcMetadataExtractorStrategy = jdbcMetadataExtractorStrategy;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
	}

	@Test
	public void testValidation(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		MetadataImplementor metadata = modelScope.getDomainModel();
		metadata.orderColumns( false );
		metadata.validate();

		// create the schema
		createSchema( metadata );

		try {
			doValidation( registryScope.getRegistry(), metadata );
		}
		finally {
			dropSchema( metadata );
		}
	}

	private void doValidation(ServiceRegistry serviceRegistry, MetadataImplementor metadata) {
		serviceRegistry.requireService( SchemaManagementTool.class )
				.getSchemaValidator( null )
				.doValidation( metadata, executionOptions( serviceRegistry ), ContributableMatcher.ALL );
	}

	private ExecutionOptions executionOptions(ServiceRegistry serviceRegistry) {
		return new ExecutionOptions() {
			final Map<String, Object> settings = serviceRegistry.requireService( ConfigurationService.class ).getSettings();

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
	}

	private void createSchema(MetadataImplementor metadata) {
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private void dropSchema(MetadataImplementor metadata) {
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "Translation")
	@Table(name = "translation_tbl")
	public static class Translation {
		@Id
		public Integer id;
		@JdbcTypeCode(Types.LONGVARCHAR)
		String text;
	}
}
