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
package org.hibernate.engine.jdbc.spi;

import org.jboss.logging.Logger;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Centralize logging for SQL statements.
 *
 * @author Steve Ebersole
 */
public class SqlStatementLogger {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, "org.hibernate.SQL");

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

