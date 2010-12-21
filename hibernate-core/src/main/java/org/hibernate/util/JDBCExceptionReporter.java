/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.util;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

public final class JDBCExceptionReporter {

    private static final Logger LOG = org.jboss.logging.Logger.getMessageLogger(Logger.class,
                                                                                JDBCExceptionReporter.class.getPackage().getName());
	public static final String DEFAULT_EXCEPTION_MSG = "SQL Exception";
	public static final String DEFAULT_WARNING_MSG = "SQL Warning";

	private JDBCExceptionReporter() {}

	/**
	 * Standard (legacy) behavior for logging warnings associated with a JDBC {@link Connection} and clearing them.
	 * <p/>
	 * Calls {@link #handleAndClearWarnings(Connection, WarningHandler)} using {@link #STANDARD_WARNING_HANDLER}
	 *
	 * @param connection The JDBC connection potentially containing warnings
	 */
	public static void logAndClearWarnings(Connection connection) {
		handleAndClearWarnings( connection, STANDARD_WARNING_HANDLER );
	}

	/**
	 * General purpose handling of warnings associated with a JDBC {@link Connection}.
	 *
	 * @param connection The JDBC connection potentially containing warnings
	 * @param handler The handler for each individual warning in the stack.
	 *
	 * @see #walkWarnings
	 */
	@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
	public static void handleAndClearWarnings(Connection connection, WarningHandler handler) {
		try {
			walkWarnings( connection.getWarnings(), handler );
		}
		catch ( SQLException sqle ) {
			//workaround for WebLogic
            LOG.debug(LOG.unableToLogWarnings(), sqle);
		}
		try {
			//Sybase fail if we don't do that, sigh...
			connection.clearWarnings();
		}
		catch ( SQLException sqle ) {
            LOG.debug(LOG.unableToClearWarnings(), sqle);
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
	@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
	public static void handleAndClearWarnings(Statement statement, WarningHandler handler) {
		try {
			walkWarnings( statement.getWarnings(), handler );
		}
		catch ( SQLException sqle ) {
			//workaround for WebLogic
            LOG.debug(LOG.unableToLogWarnings(), sqle);
		}
		try {
			//Sybase fail if we don't do that, sigh...
			statement.clearWarnings();
		}
		catch ( SQLException sqle ) {
            LOG.debug(LOG.unableToClearWarnings(), sqle);
		}
	}

	/**
	 * Log the given warning and all of its nested warnings, preceded with the {@link #DEFAULT_WARNING_MSG default message}
	 *
	 * @param warning The warning to log
	 *
	 * @deprecated Use {@link #walkWarnings} instead
	 */
	@Deprecated()
	@SuppressWarnings({ "UnusedDeclaration" })
	public static void logWarnings(SQLWarning warning) {
		walkWarnings( warning, STANDARD_WARNING_HANDLER );
	}

	/**
	 * Log the given warning and all of its nested warnings, preceded with the given message
	 *
	 * @param warning The warning to log
	 * @param message The prologue message
	 *
	 * @deprecated Use {@link #walkWarnings} instead
	 */
	@Deprecated()
	@SuppressWarnings({ "UnusedDeclaration" })
	public static void logWarnings(SQLWarning warning, String message) {
		final WarningHandler handler = StringHelper.isNotEmpty(message)
				? new StandardWarningHandler( message )
				: STANDARD_WARNING_HANDLER;
		walkWarnings( warning, handler );
	}

	/**
	 * Contract for handling {@link SQLWarning warnings}
	 */
	public static interface WarningHandler {
		/**
		 * Should processing be done?  Allows short-circuiting if not.
		 *
		 * @return True to process warnings, false otherwise.
		 */
		public boolean doProcess();

		/**
		 * Prepare for processing of a {@link SQLWarning warning} stack.
		 * <p/>
		 * Note that the warning here is also the first passed to {@link #handleWarning}
		 *
		 * @param warning The first warning in the stack.
		 */
		public void prepare(SQLWarning warning);

		/**
		 * Handle an individual warning in the stack.
		 *
		 * @param warning The warning to handle.
		 */
		public void handleWarning(SQLWarning warning);
	}

	/**
	 * Basic support for {@link WarningHandler} implementations which log
	 */
	public static abstract class WarningHandlerLoggingSupport implements WarningHandler {
		public final void handleWarning(SQLWarning warning) {
			StringBuffer buf = new StringBuffer(30)
					.append( "SQL Warning Code: ").append( warning.getErrorCode() )
					.append( ", SQLState: ").append( warning.getSQLState() );
			logWarning( buf.toString(), warning.getMessage() );
		}

		/**
		 * Delegate to log common details of a {@link SQLWarning warning}
		 *
		 * @param description A description of the warning
		 * @param message The warning message
		 */
		protected abstract void logWarning(String description, String message);
	}

	public static class StandardWarningHandler extends WarningHandlerLoggingSupport {
		private final String introMessage;

		public StandardWarningHandler(String introMessage) {
			this.introMessage = introMessage;
		}

		public boolean doProcess() {
            return LOG.isEnabled(WARN);
		}

		public void prepare(SQLWarning warning) {
            LOG.debug(introMessage, warning);
		}

		@Override
		protected void logWarning(String description, String message) {
            LOG.warn(description);
            LOG.warn(message);
		}
	}

	public static StandardWarningHandler STANDARD_WARNING_HANDLER = new StandardWarningHandler( DEFAULT_WARNING_MSG );

	public static void walkWarnings(SQLWarning warning, WarningHandler handler) {
		if ( warning == null || handler.doProcess() ) {
			return;
		}
		handler.prepare( warning );
		while ( warning != null ) {
			handler.handleWarning( warning );
			warning = warning.getNextWarning();
		}
	}

	public static void logExceptions(SQLException ex) {
		logExceptions(ex, null);
	}

	public static void logExceptions(SQLException ex, String message) {
        if (LOG.isEnabled(ERROR)) {
            if (LOG.isDebugEnabled()) {
				message = StringHelper.isNotEmpty(message) ? message : DEFAULT_EXCEPTION_MSG;
                LOG.debug(message, ex);
			}
			while (ex != null) {
                LOG.sqlError(ex.getErrorCode(), ex.getSQLState());
                LOG.error(ex.getMessage());
				ex = ex.getNextException();
			}
		}
	}

    /**
     * Interface defining messages that may be logged by the outer class
     */
    @MessageLogger
    interface Logger extends BasicLogger {

        @LogMessage( level = TRACE )
        @Message( value = "Evicting %s" )
        void evicting( String infoString );

        @LogMessage( level = WARN )
        @Message( value = "SQL Error: %d, SQLState: %s" )
        void sqlError( int errorCode,
                       String sqlState );

        @Message( value = "Could not clear warnings" )
        Object unableToClearWarnings();

        @Message( value = "Could not log warnings" )
        Object unableToLogWarnings();
    }
}
