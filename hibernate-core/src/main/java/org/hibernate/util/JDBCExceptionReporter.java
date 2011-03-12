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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JDBCExceptionReporter {
	public static final Logger log = LoggerFactory.getLogger(JDBCExceptionReporter.class);
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
			log.debug( "could not log warnings", sqle );
		}
		try {
			//Sybase fail if we don't do that, sigh...
			connection.clearWarnings();
		}
		catch ( SQLException sqle ) {
			log.debug( "could not clear warnings", sqle );
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
			log.debug( "could not log warnings", sqle );
		}
		try {
			//Sybase fail if we don't do that, sigh...
			statement.clearWarnings();
		}
		catch ( SQLException sqle ) {
			log.debug( "could not clear warnings", sqle );
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
			return log.isWarnEnabled();
		}

		public void prepare(SQLWarning warning) {
			log.debug( introMessage, warning );
		}

		@Override
		protected void logWarning(String description, String message) {
			log.warn( description );
			log.warn( message );
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
		if ( log.isErrorEnabled() ) {
			if ( log.isDebugEnabled() ) {
				message = StringHelper.isNotEmpty(message) ? message : DEFAULT_EXCEPTION_MSG;
				log.debug( message, ex );
			}
			while (ex != null) {
				StringBuffer buf = new StringBuffer(30)
						.append( "SQL Error: " )
				        .append( ex.getErrorCode() )
				        .append( ", SQLState: " )
				        .append( ex.getSQLState() );
				log.warn( buf.toString() );
				log.error( ex.getMessage() );
				ex = ex.getNextException();
			}
		}
	}
}






