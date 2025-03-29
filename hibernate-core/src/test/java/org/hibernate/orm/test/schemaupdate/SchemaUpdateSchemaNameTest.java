/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.tool.schema.internal.StandardTableMigrator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;

/**
 * Test to illustrate that the {@link StandardTableMigrator#sqlAlterStrings} method
 * uses legacy logic for building table names and doesn't adequately specify the catalog
 * or schema name properly.
 *
 * @author Chris Cranford
 */
@RequiresDialect( value = MySQLDialect.class, majorVersion = 5 )
@JiraKey(value = "HHH-11455")
public class SchemaUpdateSchemaNameTest {

	@BeforeAll
	public static void buildInitialSchema() {
		// Builds the initial table in the schema.
		StandardServiceRegistry ssr = null;
		try {
			final Configuration cfg = buildConfiguration( SimpleFirst.class );
			ssr = ServiceRegistryUtil.applySettings(
							new StandardServiceRegistryBuilder(
									new BootstrapServiceRegistryBuilder().build(),
									cfg.getStandardServiceRegistryBuilder().getAggregatedCfgXml()
							)
					)
					.applySettings( cfg.getProperties() )
					.build();
			cfg.buildSessionFactory( ssr ).close();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@AfterAll
	public static void cleanup() {
		// Drops the table after the sql alter test.
		StandardServiceRegistry ssr = null;
		try {
			// build simple configuration
			final Configuration cfg = buildConfiguration( SimpleFirst.class );

			// Build Standard Service Registry
			ssr = ServiceRegistryUtil.applySettings(
							new StandardServiceRegistryBuilder(
									new BootstrapServiceRegistryBuilder().build(),
									cfg.getStandardServiceRegistryBuilder().getAggregatedCfgXml()
							)
					)
					.applySettings( cfg.getProperties() )
					.build();

			try (SessionFactory sf = cfg.buildSessionFactory()) {
				Session session = sf.openSession();
				try {
					session.getTransaction().begin();
					session.createNativeQuery( "DROP TABLE Simple" ).executeUpdate();
					session.getTransaction().commit();
				}
				catch ( Throwable t ) {
					if ( session.getTransaction().isActive() ) {
						session.getTransaction().rollback();
					}
					throw t;
				}
				finally {
					session.close();
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testSqlAlterWithTableSchemaName() {
		StandardServiceRegistry ssr = null;
		try {
			final Configuration cfg = buildConfiguration( SimpleNext.class );
			ssr = ServiceRegistryUtil.applySettings(
							new StandardServiceRegistryBuilder(
									new BootstrapServiceRegistryBuilder().build(),
									cfg.getStandardServiceRegistryBuilder().getAggregatedCfgXml()
							)
					)
					.applySettings( cfg.getProperties() )
					.build();
			try (SessionFactory sf = cfg.buildSessionFactory( ssr )) {
				Session session = sf.openSession();
				try {
					session.getTransaction().begin();
					session.createQuery( "FROM Simple", SimpleNext.class ).getResultList();
					session.getTransaction().commit();
				}
				catch ( Throwable t ) {
					if ( session.getTransaction().isActive() ) {
						session.getTransaction().rollback();
					}
					throw t;
				}
				finally {
					session.close();
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private static Configuration buildConfiguration(Class<?> clazz) {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.HBM2DDL_AUTO, "update" );
		cfg.setProperty( AvailableSettings.SHOW_SQL, "true" );
		cfg.setProperty( AvailableSettings.FORMAT_SQL, "true" );
		cfg.addAnnotatedClass( clazz );
		return cfg;
	}

	@MappedSuperclass
	public static abstract class AbstractSimple {
		@Id
		private Integer id;
		private Integer value;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}
	}

	@Entity(name = "Simple")
	@Table(name = "Simple", schema = "test")
	public static class SimpleFirst extends AbstractSimple {

	}

	@Entity(name = "Simple")
	@Table(name = "Simple", schema = "test")
	public static class SimpleNext extends AbstractSimple {
		private String data;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
