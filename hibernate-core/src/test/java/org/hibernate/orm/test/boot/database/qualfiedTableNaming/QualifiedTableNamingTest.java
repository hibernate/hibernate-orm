/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.persister.entity.SingleTableEntityPersister;

import org.hibernate.testing.jdbc.JdbcMocks;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class QualifiedTableNamingTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Box.class };
	}

	@Override
	protected boolean createSchema() {
		return false;
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.DIALECT, TestDialect.class );
		settings.put( AvailableSettings.CONNECTION_PROVIDER, MockedConnectionProvider.class.getName() );
	}

	@Test
	public void testQualifiedNameSeparator() throws Exception {
		Namespace.Name namespaceName = new Namespace.Name(
				Identifier.toIdentifier( "DB1" ),
				Identifier.toIdentifier( "PUBLIC" )
		);

		String expectedName = null;

		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			if ( !namespace.getName().equals( namespaceName ) ) {
				continue;
			}

			assertEquals( 1, namespace.getTables().size() );

			expectedName = sessionFactory().getSqlStringGenerationContext().format(
					namespace.getTables().iterator().next().getQualifiedTableName()
			);
		}

		assertNotNull( expectedName );

		SingleTableEntityPersister persister = (SingleTableEntityPersister) sessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Box.class.getName());
		assertEquals( expectedName, persister.getTableName() );
	}

	@Entity(name = "Box")
	@jakarta.persistence.Table(name = "Box", schema = "PUBLIC", catalog = "DB1")
	public static class Box {
		@Id
		public Integer id;
		public String value;
	}

	public static class TestDialect extends Dialect {
		@Override
		public NameQualifierSupport getNameQualifierSupport() {
			return NameQualifierSupport.BOTH;
		}

		@Override
		public DatabaseVersion getVersion() {
			return ZERO_VERSION;
		}
	}

	public static class MockedConnectionProvider implements ConnectionProvider {
		private Connection connection;

		@Override
		public Connection getConnection() throws SQLException {
			if (connection == null) {
				connection = JdbcMocks.createConnection( "db1", 0 );
			}
			return connection;
		}

		@Override
		public void closeConnection(Connection connection) {
		}

		@Override
		public boolean supportsAggressiveRelease() {
			return false;
		}


		@Override
		public boolean isUnwrappableAs(Class<?> unwrapType) {
			return false;
		}

		@Override
		public <T> T unwrap(Class<T> unwrapType) {
			return null;
		}
	}

}
