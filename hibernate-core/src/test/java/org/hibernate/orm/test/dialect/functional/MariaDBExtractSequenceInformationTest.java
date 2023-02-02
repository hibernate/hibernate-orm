package org.hibernate.orm.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.TestForIssue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;

/**
 * @author Jan Schatteman
 */
@RequiresDialect(value = MariaDBDialect.class)
public class MariaDBExtractSequenceInformationTest {

	private final static String hhh15665SeqName = "HHH-15665-seq";

	private final static Map<String, Object> settings = Map.ofEntries(
		Map.entry( AvailableSettings.URL, Environment.getProperties().getProperty( AvailableSettings.URL ) ),
		Map.entry( AvailableSettings.USER, Environment.getProperties().getProperty( AvailableSettings.USER ) ),
		Map.entry( AvailableSettings.PASS, Environment.getProperties().getProperty( AvailableSettings.PASS ) )
	);

	@BeforeAll
	public static void setUp() throws Exception {
		doInAutoCommit( settings, "CREATE SEQUENCE IF NOT EXISTS `" + hhh15665SeqName + "`" );
	}

	@AfterAll
	public static void tearDown() throws SQLException {
		doInAutoCommit( settings, "DROP SEQUENCE IF EXISTS `" + hhh15665SeqName + "`" );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-15665" )
	public void testExtractSequenceInformationForSqlServerWithCaseSensitiveCollation() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder().applySettings( settings ).build();
		JdbcEnvironment jdbcEnvironment = ssr.getService( JdbcEnvironment.class );
		JdbcConnectionAccess bootstrapJdbcConnectionAccess = ssr.getService( JdbcServices.class ).getBootstrapJdbcConnectionAccess();

		try ( Connection connection = bootstrapJdbcConnectionAccess.obtainConnection() ) {
			Iterable<SequenceInformation> sequenceInformations = SequenceInformationExtractorMariaDBDatabaseImpl.INSTANCE.extractMetadata(
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

			Assertions.assertNotNull( sequenceInformations );

			Optional<SequenceInformation> seq = StreamSupport.stream( sequenceInformations.spliterator(), false )
					.filter(
							sequence -> hhh15665SeqName.equals( sequence.getSequenceName()
																.getSequenceName()
																.getText() )
					)
					.findFirst();

			Assertions.assertTrue( seq.isPresent(), hhh15665SeqName + " not found" );
		}
		catch ( SQLException e ) {
			Assertions.fail( "Sequence information for " + hhh15665SeqName + " was not retrieved: " + e.getMessage() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
