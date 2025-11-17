/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.jdbc.JdbcUtils;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.List;

import static org.hibernate.cfg.SchemaToolingSettings.ENABLE_SYNONYMS;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(H2Dialect.class)
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("extractorStrategies")
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = ViewValidationTest.TestEntity.class)
public class ViewValidationTest implements ServiceRegistryProducer {
	static List<JdbcMetadataAccessStrategy> extractorStrategies() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	private final JdbcMetadataAccessStrategy extractorStrategy;

	public ViewValidationTest(JdbcMetadataAccessStrategy extractorStrategy) {
		this.extractorStrategy = extractorStrategy;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder
				.applySetting( ENABLE_SYNONYMS, true )
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, extractorStrategy )
				.build();
	}

	@BeforeEach
	void setUp(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		new SchemaExport().create( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "CREATE VIEW test_synonym AS SELECT * FROM test_entity" );
			}
		} );
	}

	@AfterEach
	void tearDown(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		JdbcUtils.withConnection( registryScope.getRegistry(), connection -> {
			try (var statement = connection.createStatement()) {
				statement.execute( "DROP VIEW test_synonym CASCADE" );
			}
		} );
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testSynonymValidation(DomainModelScope modelScope) {
		new SchemaValidator().validate( modelScope.getDomainModel() );
	}

	@Entity
	@Table(name = "test_entity")
	public static class TestEntity {
		@Id
		private Long id;

		@Column(name = "the_key", nullable = false)
		private String key;

		@Column(name = "val")
		private String value;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity
	@Table(name = "test_synonym")
	private static class TestEntityWithSynonym {
		@Id
		private Long id;

		@Column(name = "the_key", nullable = false)
		private String key;

		@Column(name = "val")
		private String value;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

}
