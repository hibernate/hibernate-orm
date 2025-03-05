/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Frank Doherty
 */
@RequiresDialect(SQLServerDialect.class)
@JiraKey(value = "HHH-13141")
public class SQLServerDialectSequenceInformationTest extends BaseUnitTestCase {

	private final String DATABASE_NAME = "hibernate_orm_test_seq";

	@Before
	public void prepareTest() throws Exception {
		// Latin1_General_CS_AS is a case-sensitive collation
		doInAutoCommit(
			"DROP DATABASE " + DATABASE_NAME,
			"CREATE DATABASE " + DATABASE_NAME + " COLLATE Latin1_General_CS_AS"
		);
	}

	@After
	public void cleanupTest() throws Exception {
		doInAutoCommit(
			"DROP DATABASE " + DATABASE_NAME
		);
	}

	@Test
	public void testExtractSequenceInformationForSqlServerWithCaseSensitiveCollation() {
		String databaseNameToken = "databaseName=";
		String url = (String) Environment.getProperties().get( AvailableSettings.URL );
		String[] tokens = url.split( databaseNameToken );
		String newUrl = tokens[0] + databaseNameToken + DATABASE_NAME + ";trustServerCertificate=true";

		Dialect dialect = DialectContext.getDialect();

		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.URL, newUrl )
				// Reset the connection provider to avoid rebuilding the shared connection pool for this single test
				.applySetting( AvailableSettings.CONNECTION_PROVIDER, "" )
				.build();

		final JdbcConnectionAccess bootstrapJdbcConnectionAccess = ssr.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess();
		final Connection connection;
		try {
			connection = bootstrapJdbcConnectionAccess.obtainConnection();
		}
		catch (SQLException e) {
			throw new RuntimeException( e );
		}
		try {
			try (Statement statement = connection.createStatement()) {
				statement.execute( "CREATE SEQUENCE ITEM_SEQ START WITH 100 INCREMENT BY 10" );
			}

			JdbcEnvironment jdbcEnvironment = new JdbcEnvironmentImpl( connection.getMetaData(), dialect, bootstrapJdbcConnectionAccess );
			Iterable<SequenceInformation> sequenceInformations = SequenceInformationExtractorLegacyImpl.INSTANCE.extractMetadata(
					new ExtractionContext.EmptyExtractionContext() {
						@Override
						public Connection getJdbcConnection() {
							return connection;
						}

						@Override
						public JdbcEnvironment getJdbcEnvironment() {
							return jdbcEnvironment;
						}
					} );
			assertNotNull( sequenceInformations );
			SequenceInformation sequenceInformation = sequenceInformations.iterator().next();
			assertEquals( "ITEM_SEQ", sequenceInformation.getSequenceName().getSequenceName().getText().toUpperCase() );
			assertEquals( 100, sequenceInformation.getStartValue().intValue() );
			assertEquals( 10, sequenceInformation.getIncrementValue().intValue() );
		}
		catch ( SQLException e ) {
			log.error( e );
			fail( "Sequence information was not retrieved: " + e.getMessage() );
		}
		finally {
			if ( connection != null ) {
				try {
					bootstrapJdbcConnectionAccess.releaseConnection( connection );
				}
				catch (SQLException e) {
					// Ignore
				}
			}
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

}
