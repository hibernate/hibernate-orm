/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.util.concurrent.TimeUnit;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.service.Service;
import org.jboss.logging.Logger;

/**
 * Centralize logging for SQL statements.
 *
 * @author Steve Ebersole
 */
public class SqlStatementLogger implements Service {
	private static final Logger LOG = CoreLogging.logger( "org.hibernate.SQL" );
	private static final Logger LOG_SLOW = CoreLogging.logger( "org.hibernate.SQL_SLOW" );

	private final boolean logToStdout;
	private final boolean format;
	private final boolean highlight;

	/**
	 * Configuration value that indicates slow query. (In milliseconds) 0 - disabled.
	 */
	private final long logSlowQuery;

	/**
	 * Constructs a new {@code SqlStatementLogger} instance.
	 */
	public SqlStatementLogger() {
		this( false, false, false );
	}

	/**
	 * Constructs a new {@code SqlStatementLogger} instance.
	 *
	 * @param logToStdout Should we log to STDOUT in addition to our internal logger?
	 * @param format Should we format the statements in the console and log
	 */
	public SqlStatementLogger(boolean logToStdout, boolean format) {
		this( logToStdout, format, false );
	}

	/**
	 * Constructs a new {@code SqlStatementLogger} instance.
	 *
	 * @param logToStdout Should we log to STDOUT in addition to our internal logger?
	 * @param format Should we format the statements in the console and log
	 * @param highlight Should we highlight the statements in the console
	 */
	public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight) {
		this( logToStdout, format, highlight, 0 );
	}

	/**
	 * Constructs a new {@code SqlStatementLogger} instance.
	 *
	 * @param logToStdout Should we log to STDOUT in addition to our internal logger?
	 * @param format Should we format the statements in the console and log
	 * @param highlight Should we highlight the statements in the console
	 * @param logSlowQuery Should we logs query which executed slower than specified milliseconds. 0 - disabled.
	 */
	public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
		this.logToStdout = logToStdout;
		this.format = format;
		this.highlight = highlight;
		this.logSlowQuery = logSlowQuery;
	}

	/**
	 * Are we currently logging to stdout?
	 *
	 * @return True if we are currently logging to stdout; false otherwise.
	 */
	public boolean isLogToStdout() {
		return logToStdout;
	}

	public boolean isFormat() {
		return format;
	}

	public long getLogSlowQuery() {
		return logSlowQuery;
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
	@AllowSysOut
	public void logStatement(String statement, Formatter formatter) {
		if ( logToStdout || LOG.isDebugEnabled() ) {
			try {
				if ( format ) {
					statement = formatter.format( statement );
				}
				if ( highlight ) {
					statement = FormatStyle.HIGHLIGHT.getFormatter().format( statement );
				}
			}
			catch (RuntimeException ex) {
				LOG.debug( "Couldn't format statement", ex );
			}

			LOG.debug( statement );
			if ( logToStdout ) {
				String prefix = highlight ? "\u001b[35m[Hibernate]\u001b[0m " : "Hibernate: ";
				System.out.println( prefix + statement );
			}
		}
	}

	/**
	 * Log a slow SQL query
	 *
	 * @param sql The SQL query.
	 * @param startTimeNanos Start time in nanoseconds.
	 */
	public void logSlowQuery(final String sql, final long startTimeNanos, final JdbcSessionContext context) {
		if ( logSlowQuery >= 1 ) {
			if ( startTimeNanos <= 0 ) {
				throw new IllegalArgumentException(
						"startTimeNanos [" + startTimeNanos + "] should be greater than 0" );
			}

			final long queryExecutionMillis = elapsedFrom( startTimeNanos );

			if ( queryExecutionMillis > logSlowQuery ) {
				logSlowQueryInternal( context, queryExecutionMillis, sql );
			}
		}
	}

	private static long elapsedFrom(final long startTimeNanos) {
		return TimeUnit.NANOSECONDS.toMillis( System.nanoTime() - startTimeNanos );
	}

	@AllowSysOut
	private void logSlowQueryInternal(final JdbcSessionContext context, final long queryExecutionMillis, final String sql) {
		final String logData = "Slow query took " + queryExecutionMillis + " milliseconds [" + sql + "]";
		LOG_SLOW.info( logData );
		if ( logToStdout ) {
			System.out.println( logData );
		}
		if ( context != null ) {
			final var statisticsImplementor = context.getStatistics();
			if ( statisticsImplementor != null && statisticsImplementor.isStatisticsEnabled() ) {
				statisticsImplementor.slowQuery( sql, queryExecutionMillis );
			}
		}
	}

}
