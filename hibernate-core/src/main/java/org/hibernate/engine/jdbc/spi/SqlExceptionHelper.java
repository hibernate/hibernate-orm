/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.JDBCException;
import org.hibernate.exception.internal.SQLStateConverter;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * Helper for handling SQLExceptions in various manners.
 *
 * @author Steve Ebersole
 */
public class SqlExceptionHelper {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			SqlExceptionHelper.class.getName()
	);

	private static final String DEFAULT_EXCEPTION_MSG = "SQL Exception";
	private static final String DEFAULT_WARNING_MSG = "SQL Warning";
	private final boolean logWarnings;

	private static final SQLExceptionConverter DEFAULT_CONVERTER = new SQLStateConverter(
			new ViolatedConstraintNameExtracter() {
				public String extractConstraintName(SQLException e) {
					return null;
				}
			}
	);

	private SQLExceptionConverter sqlExceptionConverter;

	/**
	 * Create an exception helper with a default exception converter.
	 */
	public SqlExceptionHelper(boolean logWarnings) {
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
	 * <p/>
	 * NOTE : <tt>null</tt> is allowed and signifies to use the default.
	 *
	 * @param sqlExceptionConverter The converter to use.
	 */
	public void setSqlExceptionConverter(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = ( sqlExceptionConverter == null ? DEFAULT_CONVERTER : sqlExceptionConverter );
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
		return sqlExceptionConverter.convert( sqlException, message, sql );
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
	 * Contract for handling {@link SQLWarning warnings}
	 */
	public interface WarningHandler {
		/**
		 * Should processing be done? Allows short-circuiting if not.
		 *
		 * @return True to process warnings, false otherwise.
		 */
		boolean doProcess();

		/**
		 * Prepare for processing of a {@link SQLWarning warning} stack.
		 * <p/>
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
	 * Basic support for {@link WarningHandler} implementations which handle {@link SQLWarning warnings}
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
		 * Delegate to log common details of a {@link SQLWarning warning}
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
	 * <p/>
	 * Calls {@link #handleAndClearWarnings(Connection, WarningHandler)} using {@link #STANDARD_WARNING_HANDLER}
	 *
	 * @param connection The JDBC connection potentially containing warnings
	 */
	public void logAndClearWarnings(Connection connection) {
		handleAndClearWarnings( connection, STANDARD_WARNING_HANDLER, true );
	}

	public void logAndClearWarnings(Statement statement) {
		handleAndClearWarnings( statement, STANDARD_WARNING_HANDLER );
	}

	/**
	 * If logging of warnings has been enabled, extract them from the driver and log them.
	 *
	 * @param connection
	 * @param requiresWarningsReset when set to false, we might skip invoking {@link Connection#clearWarnings()} as we trust the connection pool to do it.
	 */
	public void logAndClearWarnings(
			final Connection connection,
			final boolean requiresWarningsReset) {
		handleAndClearWarnings( connection, STANDARD_WARNING_HANDLER, requiresWarningsReset );
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
		handleAndClearWarnings( connection, handler, true );
	}

	/**
	 * General purpose handling of warnings associated with a JDBC {@link Connection}.
	 *
	 * @param connection The JDBC connection potentially containing warnings
	 * @param handler The handler for each individual warning in the stack.
	 * @param requiresWarningsReset when set to false, we might skip invoking {@link Connection#clearWarnings()} as we trust the connection pool to do it.
	 *
	 * @see #walkWarnings
	 */
	@SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
	public void handleAndClearWarnings(
			Connection connection,
			WarningHandler handler,
			final boolean requiresWarningsReset) {
		//Start with a pessimistic assumption, but allow to skip if we happen to learn more
		//when (and if) we actually do retrieve the warnings:
		boolean thereMightBeWarnings = true;
		boolean thereDefinitelyAreWarnigs = false;
		if ( logWarnings ) {
			try {
				SQLWarning warnings = connection.getWarnings();
				if ( warnings == null ) {
					thereMightBeWarnings = false;
				}
				else {
					walkWarnings( warnings, handler );
					thereDefinitelyAreWarnigs = true;
				}
			}
			catch (SQLException sqle) {
				// workaround for WebLogic
				LOG.debug( "could not log warnings", sqle );
			}
		}
		//If there are warnings for sure, we prefer to clear them even when requiresWarningsReset is set
		//so to avoid duplicate warnings being logged (as the connection pool would likely do it as well)
		//Also consider: flag requiresWarningsReset is a performance tool, but if there are known warnings
		//we're not operating at peak performance anyway.
		if ( thereDefinitelyAreWarnigs || ( thereMightBeWarnings && requiresWarningsReset ) ) {
			try {
				// Sybase fail if we don't do that, sigh...
				connection.clearWarnings();
			}
			catch (SQLException sqle) {
				LOG.debug( "could not clear warnings", sqle );
			}
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
				SQLWarning warnings = statement.getWarnings();
				if ( warnings == null ) {
				}
				else {
					walkWarnings( warnings, handler );
				}
			}
			catch (SQLException sqlException) {
				// workaround for WebLogic
				LOG.debug( "could not log warnings", sqlException );
			}
		}
		//N.B. don't do the same optimisations as we do for handleAndClearWarnings(Connection, Handler):
		//clearing warnings on a statement is much cheaper than on a connection, and we can't delegate
		//this responsibility to the connection pool.
		try {
			// Sybase fail if we don't do that, sigh...
			statement.clearWarnings();
		}
		catch (SQLException sqle) {
			LOG.debug( "could not clear warnings", sqle );
		}
	}
}
