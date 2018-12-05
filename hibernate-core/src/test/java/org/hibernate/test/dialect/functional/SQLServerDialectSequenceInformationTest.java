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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

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
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Frank Doherty
 */
@RequiresDialect(value = { SQLServer2012Dialect.class })
@TestForIssue(jiraKey = "HHH-13141")
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
		String newUrl = tokens[0] + databaseNameToken + DATABASE_NAME;

		Dialect dialect = Dialect.getDialect();

		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySettings( Collections.singletonMap( AvailableSettings.URL, newUrl ) );
		StandardServiceRegistry ssr = ssrb.build();

		try ( Connection connection = ssr.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess()
				.obtainConnection() ) {

			try (Statement statement = connection.createStatement()) {
				statement.execute( "CREATE SEQUENCE ITEM_SEQ START WITH 100 INCREMENT BY 10" );
			}

			JdbcEnvironment jdbcEnvironment = new JdbcEnvironmentImpl( connection.getMetaData(), dialect );
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
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

}
