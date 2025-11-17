/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Allows the BaseCoreFunctionalTestCase to create the schema using TestEntity.  The test method validates against an
 * identical entity, but using the synonym name.
 *
 * When SYNONYM are used, the GROUPED Strategy cannot be applied because when the tableNamePattern was not provided
 * java.sql.DatabaseMetaData#getColumns(...) Oracle implementation returns only the columns related with the synonym
 *
 * @author Brett Meyer
 */
@RequiresDialects(
		value = {
				@RequiresDialect(value = OracleDialect.class),
				@RequiresDialect(value = DB2Dialect.class),
		}
)
public class SynonymValidationTest extends BaseSessionFactoryFunctionalTest {

	private StandardServiceRegistry ssr;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@BeforeAll
	public void setUp() {
		inTransaction(
				session -> {
					final String createStatement;
					if ( getDialect() instanceof OracleDialect ) {
						createStatement = "CREATE SYNONYM test_synonym FOR test_entity";
					}
					else {
						createStatement = "CREATE ALIAS test_synonym FOR test_entity";
					}
					session.createNativeQuery( createStatement ).executeUpdate();
				}
		);
	}

	@AfterAll
	public void tearDown() {
		inTransaction(
				session ->
				{
					final String dropStatement;
					if ( getDialect() instanceof OracleDialect ) {
						dropStatement = "DROP SYNONYM test_synonym FORCE";
					}
					else {
						dropStatement = "DROP ALIAS test_synonym FOR TABLE";
					}
					session.createNativeQuery( dropStatement ).executeUpdate();
				}
		);
	}

	@Test
	public void testSynonymUsingIndividuallySchemaValidator() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadataAccessStrategy.INDIVIDUALLY
				)
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntityWithSynonym.class );
			metadataSources.addAnnotatedClass( TestEntity.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-12406")
	public void testSynonymUsingDefaultStrategySchemaValidator() {
		// Hibernate should use JdbcMetadaAccessStrategy.INDIVIDUALLY when
		// AvailableSettings.ENABLE_SYNONYMS is true.
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntityWithSynonym.class );
			metadataSources.addAnnotatedClass( TestEntity.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@JiraKey(value = "HHH-12406")
	public void testSynonymUsingGroupedSchemaValidator() {
		// Hibernate should use JdbcMetadaAccessStrategy.INDIVIDUALLY when
		// AvailableSettings.ENABLE_SYNONYMS is true,
		// even if JdbcMetadaAccessStrategy.GROUPED is specified.
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				// Reset the connection provider to avoid rebuilding the shared connection pool for this single test
				.applySetting( AvailableSettings.CONNECTION_PROVIDER, "" )
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadataAccessStrategy.GROUPED
				)
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntityWithSynonym.class );
			metadataSources.addAnnotatedClass( TestEntity.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	@Table(name = "test_entity")
	private static class TestEntity {
		@Id
		private Long id;

		@Column(nullable = false)
		private String key;

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

		@Column(nullable = false)
		private String key;

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
