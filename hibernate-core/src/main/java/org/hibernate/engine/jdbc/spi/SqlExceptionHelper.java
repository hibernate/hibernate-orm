/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Objects;
import java.util.function.Supplier;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;

import static org.hibernate.engine.jdbc.spi.SQLExceptionLogging.ERROR_LOG;
import static org.hibernate.engine.jdbc.spi.SQLExceptionLogging.WARNING_LOG;
import org.jboss.logging.Logger.Level;

/**
 * Helper for handling {@link SQLException}s in various manners.
 *
 * @author Steve Ebersole
 */
public class SqlExceptionHelper {

	private final boolean logWarnings;
	private final boolean logErrors;

	private static final SQLExceptionConverter DEFAULT_CONVERTER =
			new StandardSQLExceptionConverter( new SQLStateConversionDelegate( () -> e -> null ) );

	private SQLExceptionConverter sqlExceptionConverter;

	/**
	 * Create an exception helper with a default exception converter.
	 */
	public SqlExceptionHelper( boolean logWarnings) {
		this( DEFAULT_CONVERTER, logWarnings );
	}

	/**
	 * Create an exception helper with a default exception converter.
	 */
	public SqlExceptionHelper( boolean logWarnings, boolean logErrors) {
		this( DEFAULT_CONVERTER, logWarnings, logErrors );
	}

	/**
	 * Create an exception helper with a specific exception converter.
	 *
	 * @param sqlExceptionConverter The exception converter to use.
	 */
	public SqlExceptionHelper(SQLExceptionConverter sqlExceptionConverter, boolean logWarnings) {
		this( sqlExceptionConverter, logWarnings, true );
	}

	/**
	 * Create an exception helper with a specific exception converter.
	 *
	 * @param sqlExceptionConverter The exception converter to use.
	 */
	public SqlExceptionHelper(SQLExceptionConverter sqlExceptionConverter, boolean logWarnings, boolean logErrors) {
		this.sqlExceptionConverter = sqlExceptionConverter;
		this.logWarnings = logWarnings;
		this.logErrors = logErrors;
	}

	/**
	 * Access the current exception converter being used internally.
	 *
	 * @return The current exception converter.
	 */
	public SQLExceptionConverter getSqlExceptionConverter() {
		return sqlExceptionConverter;
	}

	/**
	 * Inject the exception converter to use.
	 *
	 * @param sqlExceptionConverter the converter to use, or {@code null} if the default converter should be used
	 */
	public void setSqlExceptionConverter(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = sqlExceptionConverter == null ? DEFAULT_CONVERTER : sqlExceptionConverter;
	}

	// SQLException ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Convert an SQLException using the current converter, doing some logging first.
	 *
	 * @param sqlException The exception to convert
	 * @param message An error message.
	 *
	 * @return The converted exception
	 */
	public JDBCException convert(SQLException sqlException, String message) {
		return convert( sqlException, message, "n/a" );
	}

	/**
	 * Convert an SQLException using the current converter, doing some logging first.
	 *
	 * @param sqlException The exception to convert
	 * @param message An error message.
	 * @param sql The SQL being executed when the exception occurred
	 *
	 * @return The converted exception
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql) {
		logExceptions( sqlException, message + " [" + sql + "]" );
		return sqlExceptionConverter.convert( sqlException, message + " [" + sqlException.getMessage() + "]", sql );
	}

	/**
	 * Convert an SQLException using the current converter, doing some logging first.
	 *
	 * @param sqlException The exception to convert
	 * @param messageSupplier An error message supplier.
	 * @param sql The SQL being executed when the exception occurred
	 *
	 * @return The converted exception
	 */
	public JDBCException convert(SQLException sqlException, Supplier<String> messageSupplier, String sql) {
		return convert( sqlException, messageSupplier.get(), sql );
	}

	/**
	 * Log the given (and any nested) exception.
	 *
	 * @param sqlException The exception to log
	 * @param message The message text to use as a preamble.
	 */
	public void logExceptions(SQLException sqlException, String message) {
		if ( logErrors) {
			if ( ERROR_LOG.isEnabled( Level.WARN ) ) {
				SQLException currentException = sqlException;
				while ( currentException != null ) {
					if ( !isDuplicate( currentException, sqlException ) ) {
						ERROR_LOG.logErrorCodes( sqlException.getErrorCode(), sqlException.getSQLState() );
						ERROR_LOG.warn( sqlException.getMessage() );
					}
					currentException = currentException.getNextException();
				}
			}
			if ( ERROR_LOG.isDebugEnabled() ) {
				ERROR_LOG.debug( message, sqlException );
			}
		}
	}

	private static boolean isDuplicate(SQLException currentException, SQLException baseException) {
		// iterate over all previous exceptions in the chain,
		// terminating when we encounter the current exception
		SQLException previousException = baseException;
		while ( previousException != currentException && previousException != null ) {
			if ( previousException.getErrorCode() == currentException.getErrorCode()
					&& Objects.equals( previousException.getSQLState(), currentException.getSQLState() )
					&& Objects.equals( previousException.getMessage(), currentException.getMessage() ) ) {
				// we have found a distinct exception object
				// with exactly the same information in it
				return true;
			}
			previousException = previousException.getNextException();
		}
		return false;
	}

	// SQLWarning ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Contract for handling {@linkplain SQLWarning warnings}.
	 */
	public interface WarningHandler {
		/**
		 * Should processing be done? Allows short-circuiting if not.
		 *
		 * @return True to process warnings, false otherwise.
		 */
		boolean doProcess();

		/**
		 * Prepare for processing of a {@linkplain SQLWarning warning} stack.
		 * <p>
		 * Note that the warning here is also the first passed to {@link #handleWarning}
		 *
		 * @param warning The first warning in the stack.
		 */
		void prepare(SQLWarning warning);

		/**
		 * Handle an individual warning in the stack.
		 *
		 * @param warning The warning to handle.
		 */
		void handleWarning(SQLWarning warning);
	}

	/**
	 * Basic support for {@link WarningHandler} implementations which handle {@linkplain SQLWarning warnings}
	 */
	public abstract static class WarningHandlerLoggingSupport implements WarningHandler {
		@Override
		public void handleWarning(SQLWarning warning) {
			logWarning(
					"SQL Warning Code: " + warning.getErrorCode() + ", SQLState: " + warning.getSQLState(),
					warning.getMessage()
			);
		}

		/**
		 * Delegate to log common details of a {@linkplain SQLWarning warning}
		 *
		 * @param description A description of the warning
		 * @param message The warning message
		 */
		protected abstract void logWarning(String description, String message);
	}

	/**
	 * Standard SQLWarning handler for logging warnings
	 */
	public static class StandardWarningHandler extends WarningHandlerLoggingSupport {
		private final String introMessage;

		/**
		 * Creates a StandardWarningHandler
		 *
		 * @param introMessage The introduction message for the hierarchy
		 */
		public StandardWarningHandler(String introMessage) {
			this.introMessage = introMessage;
		}

		@Override
		public boolean doProcess() {
			return WARNING_LOG.isEnabled( Level.WARN );
		}

		@Override
		public void prepare(SQLWarning warning) {
			WARNING_LOG.debug( introMessage, warning );
		}

		@Override
		public final void handleWarning(SQLWarning warning) {
			WARNING_LOG.logErrorCodes( warning.getErrorCode(), warning.getSQLState() );
			WARNING_LOG.warn( warning.getMessage() );
		}

		@Override
		protected void logWarning(String description, String message) {
			WARNING_LOG.warn( description );
			WARNING_LOG.warn( message );
		}
	}

	/**
	 * Static access to the standard handler for logging warnings
	 */
	public static final StandardWarningHandler STANDARD_WARNING_HANDLER =
			new StandardWarningHandler( "SQL Warning" );

	/**
	 * Generic algorithm to walk the hierarchy of SQLWarnings
	 *
	 * @param warning The warning to walk
	 * @param handler The handler
	 */
	public void walkWarnings(SQLWarning warning, WarningHandler handler) {
		if ( warning != null && handler.doProcess() ) {
			handler.prepare( warning );
			while ( warning != null ) {
				handler.handleWarning( warning );
				warning = warning.getNextWarning();
			}
		}
	}

	/**
	 * Standard (legacy) behavior for logging warnings associated with a JDBC {@link Connection} and clearing them.
	 * <p>
	 * Calls {@link #handleAndClearWarnings(Connection, WarningHandler)} using {@link #STANDARD_WARNING_HANDLER}
	 *
	 * @param connection The JDBC connection potentially containing warnings
	 */
	public void logAndClearWarnings(Connection connection) {
		handleAndClearWarnings( connection, STANDARD_WARNING_HANDLER );
	}

	public void logAndClearWarnings(Statement statement) {
		handleAndClearWarnings( statement, STANDARD_WARNING_HANDLER );
	}

	/**
	 * General purpose handling of warnings associated with a JDBC {@link Connection}.
	 *
	 * @param connection The JDBC connection potentially containing warnings
	 * @param handler The handler for each individual warning in the stack.
	 *
	 * @see #walkWarnings
	 */
	public void handleAndClearWarnings(Connection connection, WarningHandler handler) {
		try {
			if ( logWarnings ) {
				walkWarnings( connection.getWarnings(), handler );
			}
		}
		catch (SQLException sqle) {
			// workaround for WebLogic
			WARNING_LOG.couldNotLogWarnings( sqle );
		}
		try {
			// Sybase fail if we don't do that, sigh...
			connection.clearWarnings();
		}
		catch (SQLException sqle) {
			WARNING_LOG.couldNotClearWarnings( sqle );
		}
	}

	/**
	 * General purpose handling of warnings associated with a JDBC {@link Statement}.
	 *
	 * @param statement The JDBC statement potentially containing warnings
	 * @param handler The handler for each individual warning in the stack.
	 *
	 * @see #walkWarnings
	 */
	public void handleAndClearWarnings(Statement statement, WarningHandler handler) {
		// See HHH-9174. Statement.getWarnings() can be an expensive call for some JDBC drivers.
		// Don't do it unless the log level would actually allow a warning to be logged.
		if ( logWarnings ) {
			try {
				walkWarnings( statement.getWarnings(), handler );
			}
			catch (SQLException sqlException) {
				// workaround for WebLogic
				WARNING_LOG.couldNotLogWarnings( sqlException );
			}
		}
		try {
			// Sybase fail if we don't do that, sigh...
			statement.clearWarnings();
		}
		catch (SQLException sqle) {
			WARNING_LOG.couldNotClearWarnings( sqle );
		}
	}
}
