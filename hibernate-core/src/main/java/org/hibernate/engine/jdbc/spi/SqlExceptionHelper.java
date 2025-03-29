/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLStateConversionDelegate;
import org.hibernate.exception.internal.StandardSQLExceptionConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * Helper for handling {@link SQLException}s in various manners.
 *
 * @author Steve Ebersole
 */
public class SqlExceptionHelper {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			SqlExceptionHelper.class.getName()
	);

	private static final String DEFAULT_EXCEPTION_MSG = "SQL Exception";
	private static final String DEFAULT_WARNING_MSG = "SQL Warning";
	private final boolean logWarnings;

	private static final SQLExceptionConverter DEFAULT_CONVERTER = new StandardSQLExceptionConverter(
			new SQLStateConversionDelegate( () -> e -> null )
	);

	private SQLExceptionConverter sqlExceptionConverter;

	/**
	 * Create an exception helper with a default exception converter.
	 */
	public SqlExceptionHelper( boolean logWarnings) {
		this( DEFAULT_CONVERTER, logWarnings );
	}

	/**
	 * Create an exception helper with a specific exception converter.
	 *
	 * @param sqlExceptionConverter The exception converter to use.
	 */
	public SqlExceptionHelper(SQLExceptionConverter sqlExceptionConverter, boolean logWarnings) {
		this.sqlExceptionConverter = sqlExceptionConverter;
		this.logWarnings = logWarnings;
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
		if ( LOG.isEnabled( Level.ERROR ) ) {
			if ( LOG.isDebugEnabled() ) {
				message = StringHelper.isNotEmpty( message ) ? message : DEFAULT_EXCEPTION_MSG;
				LOG.debug( message, sqlException );
			}
			final boolean warnEnabled = LOG.isEnabled( Level.WARN );

			List<String> previousWarnMessages = new ArrayList<>();
			List<String> previousErrorMessages = new ArrayList<>();

			while ( sqlException != null ) {
				if ( warnEnabled ) {
					String warnMessage = "SQL Error: " + sqlException.getErrorCode() + ", SQLState: " + sqlException.getSQLState();
					if ( !previousWarnMessages.contains( warnMessage ) ) {
						LOG.warn( warnMessage );
						previousWarnMessages.add( warnMessage );
					}
				}
				if ( !previousErrorMessages.contains( sqlException.getMessage() ) ) {
					LOG.error( sqlException.getMessage() );
					previousErrorMessages.add( sqlException.getMessage() );
				}
				sqlException = sqlException.getNextException();
			}
		}
	}

	// SQLWarning ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Contract for handling {@linkplain SQLWarning warnings}
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
		public final void handleWarning(SQLWarning warning) {
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
			return LOG.isEnabled( Level.WARN );
		}

		@Override
		public void prepare(SQLWarning warning) {
			LOG.debug( introMessage, warning );
		}

		@Override
		protected void logWarning(
				String description,
				String message) {
			LOG.warn( description );
			LOG.warn( message );
		}
	}

	/**
	 * Static access to the standard handler for logging warnings
	 */
	public static final StandardWarningHandler STANDARD_WARNING_HANDLER = new StandardWarningHandler(
			DEFAULT_WARNING_MSG
	);

	/**
	 * Generic algorithm to walk the hierarchy of SQLWarnings
	 *
	 * @param warning The warning to walk
	 * @param handler The handler
	 */
	public void walkWarnings(
			SQLWarning warning,
			WarningHandler handler) {
		if ( warning == null || !handler.doProcess() ) {
			return;
		}
		handler.prepare( warning );
		while ( warning != null ) {
			handler.handleWarning( warning );
			warning = warning.getNextWarning();
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
	@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
	public void handleAndClearWarnings(
			Connection connection,
			WarningHandler handler) {
		try {
			if ( logWarnings ) {
				walkWarnings( connection.getWarnings(), handler );
			}
		}
		catch (SQLException sqle) {
			// workaround for WebLogic
			LOG.debug( "could not log warnings", sqle );
		}
		try {
			// Sybase fail if we don't do that, sigh...
			connection.clearWarnings();
		}
		catch (SQLException sqle) {
			LOG.debug( "could not clear warnings", sqle );
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
	@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
	public void handleAndClearWarnings(
			Statement statement,
			WarningHandler handler) {
		// See HHH-9174.  Statement#getWarnings can be an expensive call for many JDBC libs.  Don't do it unless
		// the log level would actually allow a warning to be logged.
		if ( logWarnings ) {
			try {
				walkWarnings( statement.getWarnings(), handler );
			}
			catch (SQLException sqlException) {
				// workaround for WebLogic
				LOG.debug( "could not log warnings", sqlException );
			}
		}
		try {
			// Sybase fail if we don't do that, sigh...
			statement.clearWarnings();
		}
		catch (SQLException sqle) {
			LOG.debug( "could not clear warnings", sqle );
		}
	}
}
