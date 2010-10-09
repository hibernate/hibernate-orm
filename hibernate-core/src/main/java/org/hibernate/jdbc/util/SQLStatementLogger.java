/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.jdbc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralize logging handling for SQL statements.
 *
 * @author Steve Ebersole
 */
public class SQLStatementLogger {
	// todo : for 4.0
//	private static final Logger log = LoggerFactory.getLogger( SQLStatementLogger.class );
	// this is the legacy logging 'category'...
	private static final Logger log = LoggerFactory.getLogger( "org.hibernate.SQL" );

	private boolean logToStdout;
	private boolean formatSql;

	/**
	 * Constructs a new SQLStatementLogger instance.
	 */
	public SQLStatementLogger() {
		this( false, false );
	}

	/**
	 * Constructs a new SQLStatementLogger instance.
	 *
	 * @param logToStdout Should we log to STDOUT in addition to our internal logger.
	 * @param formatSql Should we format SQL ('prettify') prior to logging.
	 */
	public SQLStatementLogger(boolean logToStdout, boolean formatSql) {
		this.logToStdout = logToStdout;
		this.formatSql = formatSql;
	}

	/**
	 * Getter for property 'logToStdout'.
	 * @see #setLogToStdout
	 *
	 * @return Value for property 'logToStdout'.
	 */
	public boolean isLogToStdout() {
		return logToStdout;
	}

	/**
	 * Setter for property 'logToStdout'.
	 *
	 * @param logToStdout Value to set for property 'logToStdout'.
	 */
	public void setLogToStdout(boolean logToStdout) {
		this.logToStdout = logToStdout;
	}

	/**
	 * Getter for property 'formatSql'.
	 * @see #setFormatSql
	 *
	 * @return Value for property 'formatSql'.
	 */
	public boolean isFormatSql() {
		return formatSql;
	}

	/**
	 * Setter for property 'formatSql'.
	 *
	 * @param formatSql Value to set for property 'formatSql'.
	 */
	public void setFormatSql(boolean formatSql) {
		this.formatSql = formatSql;
	}

	/**
	 * Log a SQL statement string.
	 *
	 * @param statement The SQL statement.
	 * @param style The requested formatting style.
	 */
	public void logStatement(String statement, FormatStyle style) {
		if ( log.isDebugEnabled() || logToStdout ) {
			style = determineActualStyle( style );
			statement = style.getFormatter().format( statement );
		}
		log.debug( statement );
		if ( logToStdout ) {
			System.out.println( "Hibernate: " + statement );
		}
	}

	private FormatStyle determineActualStyle(FormatStyle style) {
		return formatSql ? style : FormatStyle.NONE;
	}
}
