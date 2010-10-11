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
package org.hibernate.service.jdbc.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.JDBCException;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.SQLStateConverter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.util.StringHelper;

/**
 * Helper for handling SQLExceptions in various manners.
 *
 * @author Steve Ebersole
 */
public class SQLExceptionHelper {
	private static final Logger log = LoggerFactory.getLogger( SQLExceptionHelper.class );

	public static final String DEFAULT_EXCEPTION_MSG = "SQL Exception";
	public static final String DEFAULT_WARNING_MSG = "SQL Warning";

	public static final SQLExceptionConverter DEFAULT_CONVERTER = new SQLStateConverter(
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
	public SQLExceptionHelper() {
		sqlExceptionConverter = DEFAULT_CONVERTER;
	}

	/**
	 * Create an exception helper with a specific exception converter.
	 *
	 * @param sqlExceptionConverter The exception converter to use.
	 */
	public SQLExceptionHelper(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = sqlExceptionConverter;
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

	/**
	 * Convert an SQLException using the current converter, doing some logging first.
	 *
	 * @param sqle The exception to convert
	 * @param message An error message.
	 * @return The converted exception
	 */
	public JDBCException convert(SQLException sqle, String message) {
		return convert( sqle, message, "n/a" );
	}

	/**
	 * Convert an SQLException using the current converter, doing some logging first.
	 *
	 * @param sqle The exception to convert
	 * @param message An error message.
	 * @param sql The SQL being executed when the exception occurred
	 * @return The converted exception
	 */
	public JDBCException convert(SQLException sqle, String message, String sql) {
		logExceptions( sqle, message + " [" + sql + "]" );
		return sqlExceptionConverter.convert( sqle, message, sql );
	}

	/**
	 * Log any {@link java.sql.SQLWarning}s registered with the connection.
	 *
	 * @param connection The connection to check for warnings.
	 */
	public void logAndClearWarnings(Connection connection) {
		if ( log.isWarnEnabled() ) {
			try {
				logWarnings( connection.getWarnings() );
			}
			catch ( SQLException sqle ) {
				//workaround for WebLogic
				log.debug( "could not log warnings", sqle );
			}
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
	 * Log the given (and any nested) warning.
	 *
	 * @param warning The warning
	 */
	public void logWarnings(SQLWarning warning) {
		logWarnings( warning, null );
	}

	/**
	 * Log the given (and any nested) warning.
	 *
	 * @param warning The warning
	 * @param message The message text to use as a preamble.
	 */
	public void logWarnings(SQLWarning warning, String message) {
		if ( log.isWarnEnabled() ) {
			if ( log.isDebugEnabled() && warning != null ) {
				message = StringHelper.isNotEmpty( message ) ? message : DEFAULT_WARNING_MSG;
				log.debug( message, warning );
			}
			while ( warning != null ) {
				StringBuffer buf = new StringBuffer( 30 )
						.append( "SQL Warning: " )
						.append( warning.getErrorCode() )
						.append( ", SQLState: " )
						.append( warning.getSQLState() );
				log.warn( buf.toString() );
				log.warn( warning.getMessage() );
				warning = warning.getNextWarning();
			}
		}
	}

	/**
	 * Log the given (and any nested) exception.
	 *
	 * @param sqlException The exception to log
	 */
	public void logExceptions(SQLException sqlException) {
		logExceptions( sqlException, null );
	}

	/**
	 * Log the given (and any nested) exception.
	 *
	 * @param sqlException The exception to log
	 * @param message The message text to use as a preamble.
	 */
	public void logExceptions(SQLException sqlException, String message) {
		if ( log.isErrorEnabled() ) {
			if ( log.isDebugEnabled() ) {
				message = StringHelper.isNotEmpty( message ) ? message : DEFAULT_EXCEPTION_MSG;
				log.debug( message, sqlException );
			}
			while ( sqlException != null ) {
				StringBuffer buf = new StringBuffer( 30 )
						.append( "SQL Error: " )
						.append( sqlException.getErrorCode() )
						.append( ", SQLState: " )
						.append( sqlException.getSQLState() );
				log.warn( buf.toString() );
				log.error( sqlException.getMessage() );
				sqlException = sqlException.getNextException();
			}
		}
	}
}
