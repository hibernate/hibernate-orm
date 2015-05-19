/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.spi;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.CoreLogging;

import org.jboss.logging.Logger;

/**
 * Centralize logging for SQL statements.
 *
 * @author Steve Ebersole
 */
public class SqlStatementLogger {
	private static final Logger LOG = CoreLogging.logger( "org.hibernate.SQL" );

	private boolean logToStdout;
	private boolean format;

	/**
	 * Constructs a new SqlStatementLogger instance.
	 */
	public SqlStatementLogger() {
		this( false, false );
	}

	/**
	 * Constructs a new SqlStatementLogger instance.
	 *
	 * @param logToStdout Should we log to STDOUT in addition to our internal logger.
	 * @param format Should we format the statements prior to logging
	 */
	public SqlStatementLogger(boolean logToStdout, boolean format) {
		this.logToStdout = logToStdout;
		this.format = format;
	}

	/**
	 * Are we currently logging to stdout?
	 *
	 * @return True if we are currently logging to stdout; false otherwise.
	 */
	public boolean isLogToStdout() {
		return logToStdout;
	}

	/**
	 * Enable (true) or disable (false) logging to stdout.
	 *
	 * @param logToStdout True to enable logging to stdout; false to disable.
	 */
	public void setLogToStdout(boolean logToStdout) {
		this.logToStdout = logToStdout;
	}

	public boolean isFormat() {
		return format;
	}

	public void setFormat(boolean format) {
		this.format = format;
	}

	/**
	 * Log a SQL statement string.
	 *
	 * @param statement The SQL statement.
	 */
	public void logStatement(String statement) {
		// for now just assume a DML log for formatting
		logStatement( statement, FormatStyle.BASIC.getFormatter() );
	}

	/**
	 * Log a SQL statement string using the specified formatter
	 *
	 * @param statement The SQL statement.
	 * @param formatter The formatter to use.
	 */
	public void logStatement(String statement, Formatter formatter) {
		if ( format ) {
			if ( logToStdout || LOG.isDebugEnabled() ) {
				statement = formatter.format( statement );
			}
		}
		LOG.debug( statement );
		if ( logToStdout ) {
			System.out.println( "Hibernate: " + statement );
		}
	}
}

