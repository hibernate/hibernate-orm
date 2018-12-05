/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorLegacyImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * used driver hibernate.connection.driver_class com.microsoft.sqlserver.jdbc.SQLServerDriver
 *
 * @author Frank Doherty
 */
@RequiresDialect(value = { SQLServer2012Dialect.class })
public class SQLServerDialectSequenceInformationTest extends BaseCoreFunctionalTestCase {

	private final String DATABASE_NAME = "hibernate_orm_test_seq";

	@Before
	public void init() {
		doInAutoCommit( "DROP DATABASE " + DATABASE_NAME );

		// Latin1_General_CS_AS is a case-sensitive collation
		doInAutoCommit( "CREATE DATABASE " + DATABASE_NAME + " COLLATE Latin1_General_CS_AS" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13141")
	public void testExtractSequenceInformationForSqlServerWithCaseSensitiveCollation() {
		String databaseNameToken = "databaseName=";
		String url = (String) Environment.getProperties().get( AvailableSettings.URL );
		String[] tokens = url.split( databaseNameToken );
		String newUrl = tokens[0] + databaseNameToken + DATABASE_NAME;

		Dialect dialect = serviceRegistry().getService( JdbcServices.class ).getDialect();

		doInHibernate( this::sessionFactory, session -> {
			StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
			ssrb.applySettings( Collections.singletonMap( AvailableSettings.URL, newUrl ) );
			StandardServiceRegistry ssr = ssrb.build();

			try ( Connection connection = ssr.getService( JdbcServices.class )
					.getBootstrapJdbcConnectionAccess()
					.obtainConnection() ) {
				JdbcEnvironment jdbcEnvironment = new JdbcEnvironmentImpl( connection.getMetaData(), dialect );
				Iterable<SequenceInformation> information = SequenceInformationExtractorLegacyImpl.INSTANCE.extractMetadata(
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
				assertNotNull( information );
			}
			catch ( SQLException e ) {
				log.error( e );
				fail( "Sequence information was not retrieved: " + e.getMessage() );
			}
			finally {
				StandardServiceRegistryBuilder.destroy( ssr );
			}
		} );
	}

	private void doInAutoCommit(String updateQuery ) {
		try ( Connection connection = serviceRegistry().getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess()
				.obtainConnection();
			 Statement statement = connection.createStatement() ) {
			connection.setAutoCommit( true );
			statement.executeUpdate( updateQuery );
		}
		catch ( SQLException e ) {
			log.debug( e.getMessage() );
		}

	}

}
