/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.StreamSupport.stream;
import static org.hibernate.testing.transaction.TransactionUtil.doInAutoCommit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RequiresDialect(OracleDialect.class)
@JiraKey(value = "HHH-13694")
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
		assertEquals( new BigDecimal( MIN_VALUE ), sequence.getMinValue() );
	}

	@Test
	public void testExtractSequenceWithMaxValueGreaterThanLongMaxValue() throws SQLException {
		SequenceInformation sequence = fetchSequenceInformation( MAX_SEQUENCE_NAME );

		assertEquals( 1L, sequence.getIncrementValue().longValue() );
		assertEquals( new BigDecimal( MAX_VALUE ), sequence.getMaxValue() );
	}

	private SequenceInformation fetchSequenceInformation(String sequenceName) throws SQLException {
		return TransactionUtil.doWithJDBC(
				sessionFactory().getServiceRegistry(),
				connection -> {
					final JdbcEnvironment jdbcEnvironment =
							sessionFactory().getJdbcServices().getJdbcEnvironment();
					// lets skip system sequences
					Optional<SequenceInformation> foundSequence =
							stream( sequenceInformation( connection, jdbcEnvironment ).spliterator(), false )
							.filter( sequence -> isSameSequence( sequenceName, sequence ) )
							.findFirst();
					assertTrue( sequenceName + " not found", foundSequence.isPresent() );
					return foundSequence.get();
				}
		);
	}

	private static boolean isSameSequence(String sequenceName, SequenceInformation sequence) {
		return sequenceName.equals( sequence.getSequenceName().getSequenceName().getText().toUpperCase() );
	}

	private static Iterable<SequenceInformation> sequenceInformation(Connection connection, JdbcEnvironment jdbcEnvironment)
			throws SQLException {
		return SequenceInformationExtractorOracleDatabaseImpl.INSTANCE.extractMetadata(
				new ExtractionContext.EmptyExtractionContext() {
					@Override
					public Connection getJdbcConnection() {
						return connection;
					}

					@Override
					public JdbcEnvironment getJdbcEnvironment() {
						return jdbcEnvironment;
					}
				}
		);
	}
}
