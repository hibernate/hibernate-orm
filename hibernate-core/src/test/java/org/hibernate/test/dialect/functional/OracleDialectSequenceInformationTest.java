/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle12cDialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(value = { Oracle12cDialect.class })
@TestForIssue(jiraKey = "HHH-13694")
public class OracleDialectSequenceInformationTest extends BaseUnitTestCase {

	private final String SEQUENCE_NAME = "SEQ_MIN_TEST";

	@Before
	public void prepareTest() throws Exception {
		doInAutoCommit(
				"DROP SEQUENCE " + SEQUENCE_NAME,
				"CREATE SEQUENCE " + SEQUENCE_NAME + " MINVALUE -99999999999999999999999999 MAXVALUE -1 INCREMENT BY -1" );
	}

	@After
	public void cleanupTest() throws Exception {
		doInAutoCommit(
				"DROP SEQUENCE " + SEQUENCE_NAME );
	}

	@Test
	public void testExtractSequenceWithMinValueLowerThanLongMinValue() throws SQLException {
		Dialect dialect = new Oracle12cDialect();

		StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		StandardServiceRegistry ssr = ssrb.build();

		try ( Connection connection = ssr.getService( JdbcServices.class )
				.getBootstrapJdbcConnectionAccess()
				.obtainConnection() ) {

			JdbcEnvironment jdbcEnvironment = new JdbcEnvironmentImpl( connection.getMetaData(), dialect );
			SequenceInformationExtractorOracleDatabaseImpl sequenceExtractor = SequenceInformationExtractorOracleDatabaseImpl.INSTANCE;
			Iterable<SequenceInformation> sequenceInformations = sequenceExtractor.extractMetadata(
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

			// lets skip system sequences
			Optional<SequenceInformation> foundSequence = StreamSupport.stream( sequenceInformations.spliterator(), false )
					.filter( sequence -> SEQUENCE_NAME.equals( sequence.getSequenceName().getSequenceName().getText().toUpperCase() ) )
					.findFirst();

			assertTrue( SEQUENCE_NAME + " not found", foundSequence.isPresent() );

			SequenceInformation sequence = foundSequence.get();

			assertEquals( -1L, sequence.getIncrementValue().longValue() );
			assertEquals( Long.MIN_VALUE, sequence.getMinValue().longValue() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

}
