/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.InstantiationException;
import org.hibernate.Internal;
import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.ExecuteUpdateResultCheckStyle;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Holds various often used {@link Expectation} definitions.
 *
 * @author Steve Ebersole
 */
public class Expectations {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( Expectations.class );

	static final SqlExceptionHelper sqlExceptionHelper = new SqlExceptionHelper( false );

	// Base Expectation impls ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link RowCount}
	 */
	@Deprecated(since = "6.5")
	public static class BasicExpectation implements Expectation {
		private final int expected;

		protected BasicExpectation(int expectedRowCount) {
			expected = expectedRowCount;
			if ( expectedRowCount < 0 ) {
				throw new IllegalArgumentException( "Expected row count must be greater than zero" );
			}
		}

		@Override
		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql) {
			final int result = determineRowCount( rowCount, statement );
			if ( batchPosition < 0 ) {
				checkNonBatched( expected, result, sql );
			}
			else {
				checkBatched( expected, result, batchPosition, sql );
			}
		}

		protected int determineRowCount(int reportedRowCount, PreparedStatement statement) {
			return reportedRowCount;
		}
	}

	/**
	 * @deprecated Use {@link OutParameter}
	 */
	@Deprecated(since = "6.5")
	public static class BasicParamExpectation extends BasicExpectation {
		private final int parameterPosition;

		protected BasicParamExpectation(int expectedRowCount, int parameterPosition) {
			super( expectedRowCount );
			this.parameterPosition = parameterPosition;
		}

		@Override
		public int getNumberOfParametersUsed() {
			return 1;
		}

		@Override
		public int prepare(PreparedStatement statement) throws SQLException, HibernateException {
			toCallableStatement( statement ).registerOutParameter( parameterPosition, Types.NUMERIC );
			return 1;
		}

		@Override
		public boolean canBeBatched() {
			return false;
		}

		@Override
		protected int determineRowCount(int reportedRowCount, PreparedStatement statement) {
			try {
				return toCallableStatement( statement ).getInt( parameterPosition );
			}
			catch ( SQLException sqle ) {
				sqlExceptionHelper.logExceptions( sqle, "could not extract row counts from CallableStatement" );
				throw new GenericJDBCException( "could not extract row counts from CallableStatement", sqle );
			}
		}
	}

	static CallableStatement toCallableStatement(PreparedStatement statement) {
		if ( statement instanceof CallableStatement ) {
			return (CallableStatement) statement;
		}
		else {
			throw new HibernateException( "Expectation.OutParameter operates exclusively on CallableStatements: "
					+ statement.getClass() );
		}
	}

	static void checkBatched(int expected, int rowCount, int batchPosition, String sql) {
		if ( rowCount == -2 ) {
			LOG.debugf( "Success of batch update unknown: %s", batchPosition );
		}
		else if ( rowCount == -3 ) {
			throw new BatchFailedException( "Batch update failed: " + batchPosition );
		}
		else if ( expected > rowCount ) {
			throw new StaleStateException(
					"Batch update returned unexpected row count from update ["
							+ batchPosition + "]; actual row count: " + rowCount
							+ "; expected: " + 1 + "; statement executed: "
							+ sql
			);
		}
		else if ( expected < rowCount ) {
			String msg = "Batch update returned unexpected row count from update [" +
					batchPosition + "]; actual row count: " + rowCount +
					"; expected: " + 1;
			throw new BatchedTooManyRowsAffectedException( msg, 1, rowCount, batchPosition );
		}
	}

	static void checkNonBatched(int expected, int rowCount, String sql) {
		if ( expected > rowCount ) {
			throw new StaleStateException(
					"Unexpected row count: " + rowCount + "; expected: " + 1
							+ "; statement executed: " + sql
			);
		}
		if ( expected < rowCount ) {
			String msg = "Unexpected row count: " + rowCount + "; expected: " + 1;
			throw new TooManyRowsAffectedException( msg, 1, rowCount );
		}
	}

	// Various Expectation instances ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * @deprecated Use {@link Expectation.None}
	 */
	@Deprecated(since = "6.5")
	public static final Expectation NONE = new Expectation.None();

	/**
	 * @deprecated Use {@link Expectation.RowCount}
	 */
	@Deprecated(since = "6.5")
	public static final Expectation BASIC = new Expectation.RowCount();

	/**
	 * @deprecated Use {@link Expectation.OutParameter}
	 */
	@Deprecated(since = "6.5")
	public static final Expectation PARAM = new Expectation.OutParameter();

	@Internal
	public static Expectation createExpectation(Class<? extends Expectation> expectation, boolean callable) {
		if ( expectation == null ) {
			expectation = callable ? Expectation.OutParameter.class : Expectation.RowCount.class;
		}
		try {
			return expectation.newInstance();
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate Expectation", expectation, e );
		}
	}

	@Deprecated(since = "6.5", forRemoval = true)
	public static Expectation appropriateExpectation(ExecuteUpdateResultCheckStyle style) {
		switch ( style ) {
			case NONE:
				return NONE;
			case COUNT:
				return BASIC;
			case PARAM:
				return PARAM;
			default:
				throw new AssertionFailure( "unknown result check style: " + style );
		}
	}

	private Expectations() {
	}

	// Unused, for removal ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Deprecated(since = "6.5", forRemoval = true)
	public static final int USUAL_EXPECTED_COUNT = 1;

	@Deprecated(since = "6.5", forRemoval = true)
	public static final int USUAL_PARAM_POSITION = 1;

}
