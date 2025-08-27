/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.exception.GenericJDBCException;

import static org.hibernate.jdbc.Expectations.checkBatched;
import static org.hibernate.jdbc.Expectations.checkNonBatched;
import static org.hibernate.jdbc.Expectations.sqlExceptionHelper;
import static org.hibernate.jdbc.Expectations.toCallableStatement;

/**
 * Defines an expected DML operation outcome.
 * Used to verify that a JDBC operation completed successfully.
 * <p>
 * The two standard implementations are {@link RowCount} for
 * row count checking, and {@link OutParameter} for checking
 * the return code assigned to an output parameter of a
 * {@link CallableStatement}. Custom implementations are
 * permitted.
 * <p>
 * An {@code Expectation} is usually selected via an annotation,
 * for example:
 * <pre>
 * &#064;Entity
 * &#064;SQLUpdate(sql = "update Record set uid = gen_random_uuid(), whatever = ? where id = ?",
 *            verify = Expectation.RowCount.class)
 * class Record { ... }
 * </pre>
 *
 * @see org.hibernate.annotations.SQLInsert#verify
 * @see org.hibernate.annotations.SQLUpdate#verify
 * @see org.hibernate.annotations.SQLDelete#verify
 * @see org.hibernate.annotations.SQLDeleteAll#verify
 *
 * @author Steve Ebersole
 */
public interface Expectation {

	/**
	 * Is it acceptable to combine this expectation with JDBC
	 * {@linkplain PreparedStatement#executeBatch() statement batching}?
	 * If this method returns {@code false}, the use of batch updates
	 * is disabled.
	 *
	 * @return True if batching can be combined with this expectation;
	 *         false otherwise.
	 *
	 * @see PreparedStatement#executeBatch()
	 */
	default boolean canBeBatched() {
		return true;
	}

	/**
	 * The number of JDBC parameters this expectation uses. For example,
	 * {@link OutParameter} requires a single OUT parameter for reading
	 * back the number of affected rows.
	 */
	default int getNumberOfParametersUsed() {
		return 0;
	}

	/**
	 * Perform verification of the outcome of the JDBC operation based
	 * on the type of expectation defined, after execution of the given
	 * {@link PreparedStatement}. When a SQL statement is executed via
	 * {@link PreparedStatement#executeUpdate()}, {@code verifyOutcome()}
	 * is called exactly once. When {@link PreparedStatement#executeBatch()}
	 * is used to execute a batch update, this method is called once for
	 * each element of the batch.
	 * <ul>
	 * <li>The argument to {@code rowCount} is usually the number of
	 *     table rows affected by execution of the SQL statement via
	 *     {@code executeUpdate()}. However, in the case where
	 *     {@code executeBatch()} is used to execute a batch update,
	 *     it might be {@link PreparedStatement#EXECUTE_FAILED} or
	 *     {@link PreparedStatement#SUCCESS_NO_INFO}.
	 * <li>The argument to {@code batchPosition} is negative unless
	 *     {@code executeBatch()} is used to execute a batch update,
	 *     in which case it is the position within the batch of the
	 *     row count being verified.
	 * </ul>
	 *
	 * @param rowCount The RDBMS reported "number of rows affected"
	 * @param statement The statement representing the operation
	 * @param batchPosition The position in the batch (if batching),
	 *                      or {@code -1} if not part of a batch
	 * @param sql The SQL backing the prepared statement, for error
	 *            reporting and logging purposes
	 * @throws SQLException Exception from the JDBC driver.
	 * @throws HibernateException Problem processing the outcome.
	 *
	 * @see PreparedStatement#executeUpdate()
	 * @see PreparedStatement#executeBatch()
	 */
	void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql)
			throws SQLException, HibernateException;

	/**
	 * Perform any special statement preparation, for example,
	 * registration of OUT parameters.
	 *
	 * @param statement The statement to be prepared
	 * @return The number of bind positions consumed (if any)
	 * @throws SQLException Exception from the JDBC driver
	 * @throws HibernateException Problem performing preparation.
	 *
	 * @see CallableStatement#registerOutParameter(int, int)
	 */
	default int prepare(PreparedStatement statement)
			throws SQLException, HibernateException {
		return 0;
	}

	/**
	 * Check that this implementation is compatible with the kind of
	 * {@link PreparedStatement} it will be called with. Implementors
	 * should throw a {@link MappingException} if the configuration
	 * is not supported. This operation is called when Hibernate
	 * starts up, so that incompatibilities are detected and reported
	 * as early as possible.
	 *
	 * @param callable true if this {@code Expectation} will be called
	 *                 with a {@link CallableStatement}.
	 *
	 * @since 6.5
	 */
	default void validate(boolean callable) throws MappingException {
	}

	/**
	 * No return code checking. Might mean that no checks are required, or that
	 * failure is indicated by a {@link java.sql.SQLException} being thrown, for
	 * example, by a {@link java.sql.CallableStatement stored procedure} which
	 * performs explicit checks.
	 *
	 * @since 6.5
	 */
	class None implements Expectation {
		public static final None INSTANCE = new None();
		@Override
		public void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql) {
			// nothing to do
		}
	}

	/**
	 * Row count checking. A row count is an integer value returned by
	 * {@link java.sql.PreparedStatement#executeUpdate()} or
	 * {@link java.sql.Statement#executeBatch()}. The row count is checked
	 * against an expected value. For example, the expected row count for
	 * an {@code INSERT} statement is always 1.
	 *
	 * @since 6.5
	 */
	class RowCount implements Expectation {
		@Override
		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql) {
			if ( batchPosition < 0 ) {
				checkNonBatched( expectedRowCount(), rowCount, sql );
			}
			else {
				checkBatched( expectedRowCount(), rowCount, batchPosition, sql );
			}
		}

		protected int expectedRowCount() {
			return 1;
		}
	}

	/**
	 * Essentially identical to {@link RowCount} except that the row count
	 * is obtained via an output parameter of a {@linkplain CallableStatement
	 * stored procedure}.
	 * <p>
	 * Statement batching is disabled when {@code OutParameter} is used.
	 *
	 * @since 6.5
	 */
	class OutParameter implements Expectation {
		@Override
		public final void verifyOutcome(int rowCount, PreparedStatement statement, int batchPosition, String sql) {
			final int result;
			try {
				result = toCallableStatement( statement ).getInt( parameterIndex() );
			}
			catch ( SQLException sqle ) {
				sqlExceptionHelper.logExceptions( sqle, "Could not extract row count from CallableStatement" );
				throw new GenericJDBCException( "Could not extract row count from CallableStatement", sqle );
			}
			if ( batchPosition < 0 ) {
				checkNonBatched( expectedRowCount(), result, sql );
			}
			else {
				checkBatched( expectedRowCount(), result, batchPosition, sql );
			}
		}

		@Override
		public void validate(boolean callable) throws MappingException {
			if ( !callable ) {
				throw new MappingException( "Expectation.OutParameter operates exclusively on CallableStatements" );
			}
		}

		@Override
		public int getNumberOfParametersUsed() {
			return 1;
		}

		@Override
		public int prepare(PreparedStatement statement) throws SQLException, HibernateException {
			toCallableStatement( statement ).registerOutParameter( parameterIndex(), Types.NUMERIC );
			return 1;
		}

		@Override
		public boolean canBeBatched() {
			return false;
		}

		protected int parameterIndex() {
			return 1;
		}

		protected int expectedRowCount() {
			return 1;
		}
	}

}
