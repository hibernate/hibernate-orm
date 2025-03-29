/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemavalidation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.SkipForDialect;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialects;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@RequiresDialects({
		@RequiresDialect(PostgreSQLDialect.class),
		@RequiresDialect(H2Dialect.class)
})
@SkipForDialect(value = CockroachDialect.class, comment = "https://github.com/cockroachdb/cockroach/issues/10028")
public class ViewValidationTest extends BaseCoreFunctionalTestCase {
	private StandardServiceRegistry ssr;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {TestEntity.class};
	}

	@Before
	public void setUp() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "CREATE VIEW test_synonym AS SELECT * FROM test_entity" ).executeUpdate();
		} );
	}

	@After
	public void tearDown() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.createNativeQuery( "DROP VIEW test_synonym CASCADE" ).executeUpdate();
		} );
	}

	@Test
	public void testSynonymUsingGroupedSchemaValidator() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadaAccessStrategy.GROUPED
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
	public void testSynonymUsingIndividuallySchemaValidator() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadaAccessStrategy.INDIVIDUALLY
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
