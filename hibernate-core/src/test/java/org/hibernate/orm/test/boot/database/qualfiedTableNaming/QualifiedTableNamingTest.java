/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.testing.jdbc.JdbcMocks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.hibernate.cfg.JdbcSettings.CONNECTION_PROVIDER;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settings = {
		@Setting(name=DIALECT, value = "org.hibernate.orm.test.boot.database.qualfiedTableNaming.QualifiedTableNamingTest$TestDialect"),
		@Setting(name = CONNECTION_PROVIDER, value = "org.hibernate.orm.test.boot.database.qualfiedTableNaming.QualifiedTableNamingTest$MockedConnectionProvider")
})
@DomainModel(annotatedClasses = QualifiedTableNamingTest.Box.class)
@SessionFactory(exportSchema = false)
public class QualifiedTableNamingTest {

	@Test
	public void testQualifiedNameSeparator(DomainModelScope modelScope, SessionFactoryScope factoryScope) throws Exception {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		Namespace.Name namespaceName = new Namespace.Name(
				Identifier.toIdentifier( "DB1" ),
				Identifier.toIdentifier( "PUBLIC" )
		);

		String expectedName = null;

		for ( Namespace namespace : modelScope.getDomainModel().getDatabase().getNamespaces() ) {
			if ( !namespace.getName().equals( namespaceName ) ) {
				continue;
			}

			assertEquals( 1, namespace.getTables().size() );

			final SqlStringGenerationContext generationContext = sessionFactory.getSqlStringGenerationContext();
			expectedName = generationContext.format( namespace.getTables().iterator().next().getQualifiedTableName() );
		}


		assertNotNull( expectedName );

		SingleTableEntityPersister persister = (SingleTableEntityPersister) sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( Box.class.getName());
		assertEquals( expectedName, persister.getTableName() );
	}

	@Entity(name = "Box")
	@Table(name = "Box", schema = "PUBLIC", catalog = "DB1")
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
