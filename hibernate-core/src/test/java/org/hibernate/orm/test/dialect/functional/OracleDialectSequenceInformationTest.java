/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorOracleDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static java.util.stream.StreamSupport.stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresDialect(OracleDialect.class)
@JiraKey(value = "HHH-13694")
@SessionFactory
public class OracleDialectSequenceInformationTest {

	private static final String MIN_SEQUENCE_NAME = "SEQ_MIN_TEST";
	private static final String MAX_SEQUENCE_NAME = "SEQ_MAX_TEST";
	private static final String MIN_VALUE = "-99999999999999999999999999";
	private static final String MAX_VALUE =  "99999999999999999999999999";

	@BeforeAll
	public void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inSession( session ->
			session.doWork(  connection -> {
				try (PreparedStatement ps = connection.prepareStatement( "CREATE SEQUENCE " + MIN_SEQUENCE_NAME + " MINVALUE " + MIN_VALUE + " MAXVALUE -1 INCREMENT BY -1" )) {
					ps.execute();
				}
				try (PreparedStatement ps = connection.prepareStatement( "CREATE SEQUENCE " + MAX_SEQUENCE_NAME + " MINVALUE 0 MAXVALUE " + MAX_VALUE + " INCREMENT BY 1" ) ) {
					ps.execute();
				}
			} )
		);
	}

	@AfterAll
	public void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inSession( session ->
			session.doWork(  connection -> {
				try (PreparedStatement ps = connection.prepareStatement( "DROP SEQUENCE " + MIN_SEQUENCE_NAME )) {
					ps.execute();
				}
				try (PreparedStatement ps = connection.prepareStatement( "DROP SEQUENCE " + MAX_SEQUENCE_NAME )) {
					ps.execute();
				}
			} )
		);
	}

	@Test
	public void testExtractSequenceWithMinValueLowerThanLongMinValue(SessionFactoryScope scope) {
		SequenceInformation sequence = fetchSequenceInformation( MIN_SEQUENCE_NAME, scope.getSessionFactory() );

		assertEquals( -1L, sequence.getIncrementValue().longValue() );
		assertEquals( new BigDecimal( MIN_VALUE ), sequence.getMinValue() );
	}

	@Test
	public void testExtractSequenceWithMaxValueGreaterThanLongMaxValue(SessionFactoryScope scope) {
		SequenceInformation sequence = fetchSequenceInformation( MAX_SEQUENCE_NAME, scope.getSessionFactory() );

		assertEquals( 1L, sequence.getIncrementValue().longValue() );
		assertEquals( new BigDecimal( MAX_VALUE ), sequence.getMaxValue() );
	}

	private SequenceInformation fetchSequenceInformation(String sequenceName, SessionFactoryImplementor sessionFactory) {
		final JdbcEnvironment jdbcEnvironment = sessionFactory.getJdbcServices().getJdbcEnvironment();
		final SequenceInformationWrapper sequenceInformationWrapper = new SequenceInformationWrapper();
		sessionFactory.openSession().doWork(   connection -> {
			// let's skip system sequences
			Optional<SequenceInformation> foundSequence =
					stream( sequenceInformation( connection, jdbcEnvironment ).spliterator(), false )
							.filter( sequence -> isSameSequence( sequenceName, sequence ) )
							.findFirst();
			assertTrue( foundSequence.isPresent(), sequenceName + " not found" );
			sequenceInformationWrapper.set( foundSequence.get() );
		} );
		return sequenceInformationWrapper.get();
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

	private static final class SequenceInformationWrapper {
		private SequenceInformation sequenceInformation;
		public SequenceInformation get() {
			return sequenceInformation;
		}
		public void set(SequenceInformation sequenceInformation) {
			this.sequenceInformation = sequenceInformation;
		}
	}
}
