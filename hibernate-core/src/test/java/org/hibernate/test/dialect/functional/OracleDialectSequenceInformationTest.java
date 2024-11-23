/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.functional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(value = { Oracle8iDialect.class })
@TestForIssue(jiraKey = "HHH-13694")
public class OracleDialectSequenceInformationTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final String MIN_SEQUENCE_NAME = "SEQ_MIN_TEST";
	private static final String MAX_SEQUENCE_NAME = "SEQ_MAX_TEST";
	private static final String MIN_VALUE = "-99999999999999999999999999";
	private static final String MAX_VALUE =  "99999999999999999999999999";

	@Before
	public void prepareTest() throws Exception {
		doInAutoCommit(
				"DROP SEQUENCE " + MIN_SEQUENCE_NAME,
				"CREATE SEQUENCE " + MIN_SEQUENCE_NAME + " MINVALUE " + MIN_VALUE + " MAXVALUE -1 INCREMENT BY -1",
				"DROP SEQUENCE " + MAX_SEQUENCE_NAME,
				"CREATE SEQUENCE " + MAX_SEQUENCE_NAME + " MINVALUE 0 MAXVALUE " + MAX_VALUE + " INCREMENT BY 1" );
	}

	@After
	public void cleanupTest() throws Exception {
		doInAutoCommit(
				"DROP SEQUENCE " + MIN_SEQUENCE_NAME,
				"DROP SEQUENCE " + MAX_SEQUENCE_NAME );
	}

	@Test
	public void testExtractSequenceWithMinValueLowerThanLongMinValue() throws SQLException {
		SequenceInformation sequence = fetchSequenceInformation( MIN_SEQUENCE_NAME );

		assertEquals( -1L, sequence.getIncrementValue().longValue() );
		assertEquals( Long.MIN_VALUE, sequence.getMinValue().longValue() );
	}

	@Test
	public void testExtractSequenceWithMaxValueGreaterThanLongMaxValue() throws SQLException {
		SequenceInformation sequence = fetchSequenceInformation( MAX_SEQUENCE_NAME );

		assertEquals( 1L, sequence.getIncrementValue().longValue() );
		assertEquals( Long.MAX_VALUE, sequence.getMaxValue().longValue() );
	}

	private SequenceInformation fetchSequenceInformation(String sequenceName) throws SQLException {
		try ( Connection connection = sessionFactory().getJdbcServices()
				.getBootstrapJdbcConnectionAccess()
				.obtainConnection() ) {
			JdbcEnvironment jdbcEnvironment = sessionFactory().getJdbcServices().getJdbcEnvironment();
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
					.filter( sequence -> sequenceName.equals( sequence.getSequenceName().getSequenceName().getText().toUpperCase() ) )
					.findFirst();

			assertTrue( sequenceName + " not found", foundSequence.isPresent() );

			return foundSequence.get();
		}
	}
}