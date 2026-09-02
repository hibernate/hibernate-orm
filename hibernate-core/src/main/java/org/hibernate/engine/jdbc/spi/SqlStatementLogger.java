/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;

import java.util.concurrent.TimeUnit;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.internal.build.AllowSysOut;
import org.hibernate.resource.jdbc.spi.JdbcSessionContext;
import org.hibernate.service.Service;
import org.jboss.logging.Logger;

/// Centralize logging for SQL statements, with some configuration:
///
/// * [#isLogToStdout()]
/// * [#isFormat()]
/// * [#isHighlight()]
/// * [#getSlowQueryThreshold()]
///
/// Some of these configuration values can be [adjusted][org.hibernate.settings.AdjustableSettings] at runtime.
///
/// @see org.hibernate.settings.AdjustableSettings#setShowSql(boolean)
/// @see org.hibernate.settings.AdjustableSettings#setFormatLoggedSql(boolean)
/// @see org.hibernate.settings.AdjustableSettings#setSlowSqlLoggingThreshold(long)
///
/// @author Steve Ebersole
public class SqlStatementLogger implements Service {
	private static final Logger SQL_LOGGER = Logger.getLogger( "org.hibernate.SQL" );
	private static final Logger SLOW_SQL_LOGGER = Logger.getLogger( "org.hibernate.SQL_SLOW" );

	private volatile boolean logToStdout;
	private volatile long slowQueryThreshold;
	private volatile boolean format;
	private final boolean highlight;

	/// Constructs a new `SqlStatementLogger` instance.
	public SqlStatementLogger() {
		this( false, false, false );
	}

	/// Constructs a new `SqlStatementLogger` instance.
	///
	/// @param logToStdout Should we log to STDOUT in addition to our internal logger?
	/// @param format Should we format the statements in the console and log
	public SqlStatementLogger(boolean logToStdout, boolean format) {
		this( logToStdout, format, false );
	}

	/// Constructs a new `SqlStatementLogger` instance.
	///
	/// @param logToStdout Should we log to STDOUT in addition to our internal logger?
	/// @param format Should we format the statements in the console and log
	/// @param highlight Should we highlight the statements in the console
	public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight) {
		this( logToStdout, format, highlight, 0 );
	}

	/// Constructs a new `SqlStatementLogger` instance.
	///
	/// @param logToStdout Should we log to STDOUT in addition to our internal logger?
	/// @param format Should we format the statements in the console and log
	/// @param highlight Should we highlight the statements in the console
	/// @param logSlowQuery Should we logs query which executed slower than specified milliseconds. 0 - disabled.
	public SqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
		this.logToStdout = logToStdout;
		this.format = format;
		this.highlight = highlight;
		this.slowQueryThreshold = logSlowQuery;
	}

	/// Whether logging SQL to [stdout][System#out] is enabled.
	///
	/// Note that SQL is always logged via logging using the `org.hibernate.SQL` category/logger name
	/// at `DEBUG` level regardless of this setting.
	public boolean isLogToStdout() {
		return logToStdout;
	}

	/// Whether SQL is formatted prior to logging.
	/// Formatting makes the SQL "pretty" rather than one-line.
	public boolean isFormat() {
		return format;
	}

	/// Whether to apply highlighting via ASCII escape codes when logging to [stdout][#isLogToStdout()].
	public boolean isHighlight() {
		return highlight;
	}

	/// Number of milliseconds a query must take for execution to be considered slow.
	/// Used in {@linkplain #logSlowQuery logging slow}
	/// Zero (0) indicates this is disabled.
	public long getSlowQueryThreshold() {
		return slowQueryThreshold;
	}

	/// @deprecated Use [#getSlowQueryThreshold()] instead.
	@Deprecated
	public long getLogSlowQuery() {
		return slowQueryThreshold;
	}

	/// Log a SQL statement string.
	///
	/// @param statement The SQL statement.
	public void logStatement(String statement) {
		// for now just assume a DML log for formatting
		logStatement( statement, FormatStyle.BASIC.getFormatter() );
	}

	/// Log a SQL statement string using the specified formatter
	///
	/// @param statement The SQL statement.
	/// @param formatter The formatter to use.
	@AllowSysOut
	public void logStatement(String statement, Formatter formatter) {
		if ( logToStdout || SQL_LOGGER.isDebugEnabled() ) {
			try {
				if ( format ) {
					statement = formatter.format( statement );
				}
				if ( highlight ) {
					statement = FormatStyle.HIGHLIGHT.getFormatter().format( statement );
				}
			}
			catch (RuntimeException ex) {
				SQL_LOGGER.debug( "Couldn't format statement", ex );
			}

			if ( SQL_LOGGER.isDebugEnabled() ) {
				SQL_LOGGER.debug( statement );
			}

			if ( logToStdout ) {
				String prefix = highlight ? "\u001b[35m[Hibernate]\u001b[0m " : "Hibernate: ";
				System.out.println( prefix + statement );
			}
		}
	}

	/// Log a slow SQL query
	///
	/// @param sql The SQL query.
	/// @param startTimeNanos Start time in nanoseconds, or `0` if timing was not started.
	public void logSlowQuery(final String sql, final long startTimeNanos, final JdbcSessionContext context) {
		if ( startTimeNanos == 0 ) {
			// slow-query logging was disabled, at least as of the time the query in question started
			return;
		}

		final long slowQueryThreshold = this.slowQueryThreshold;
		if ( slowQueryThreshold >= 1 ) {
			final long queryExecutionMillis = elapsedFrom( startTimeNanos );

			if ( queryExecutionMillis > slowQueryThreshold ) {
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
		SLOW_SQL_LOGGER.info( logData );
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

	/// Adjust whether to log to [stdout][#isLogToStdout()].
	public void setLogToStdout(boolean logToStdout) {
		this.logToStdout = logToStdout;
	}

	/// Adjust whether to [format][#isFormat()] SQL prior to logging.
	public void setFormat(boolean format) {
		this.format = format;
	}

	/// Adjust the [slow SQL threshold][#getSlowQueryThreshold()].
	public void setLogSlowQuery(long threshold) {
		if ( threshold <= 0 ) {
			this.slowQueryThreshold = 0;
		}
		else {
			this.slowQueryThreshold = threshold;
		}
	}
}
