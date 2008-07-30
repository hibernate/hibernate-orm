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
package org.hibernate.exception;

import org.hibernate.JDBCException;
import org.hibernate.util.JDBCExceptionReporter;

import java.sql.SQLException;

/**
 * Implementation of JDBCExceptionHelper.
 *
 * @author Steve Ebersole
 */
public final class JDBCExceptionHelper {

	private JDBCExceptionHelper() {
	}

	/**
	 * Converts the given SQLException into Hibernate's JDBCException hierarchy, as well as performing
	 * appropriate logging.
	 *
	 * @param converter    The converter to use.
	 * @param sqlException The exception to convert.
	 * @param message      An optional error message.
	 * @return The converted JDBCException.
	 */
	public static JDBCException convert(SQLExceptionConverter converter, SQLException sqlException, String message) {
		return convert( converter, sqlException, message, "???" );
	}

	/**
	 * Converts the given SQLException into Hibernate's JDBCException hierarchy, as well as performing
	 * appropriate logging.
	 *
	 * @param converter    The converter to use.
	 * @param sqlException The exception to convert.
	 * @param message      An optional error message.
	 * @return The converted JDBCException.
	 */
	public static JDBCException convert(SQLExceptionConverter converter, SQLException sqlException, String message, String sql) {
		JDBCExceptionReporter.logExceptions( sqlException, message + " [" + sql + "]" );
		return converter.convert( sqlException, message, sql );
	}

	/**
	 * For the given SQLException, locates the vendor-specific error code.
	 *
	 * @param sqlException The exception from which to extract the SQLState
	 * @return The error code.
	 */
	public static int extractErrorCode(SQLException sqlException) {
		int errorCode = sqlException.getErrorCode();
		SQLException nested = sqlException.getNextException();
		while ( errorCode == 0 && nested != null ) {
			errorCode = nested.getErrorCode();
			nested = nested.getNextException();
		}
		return errorCode;
	}

	/**
	 * For the given SQLException, locates the X/Open-compliant SQLState.
	 *
	 * @param sqlException The exception from which to extract the SQLState
	 * @return The SQLState code, or null.
	 */
	public static String extractSqlState(SQLException sqlException) {
		String sqlState = sqlException.getSQLState();
		SQLException nested = sqlException.getNextException();
		while ( sqlState == null && nested != null ) {
			sqlState = nested.getSQLState();
			nested = nested.getNextException();
		}
		return sqlState;
	}

	/**
	 * For the given SQLException, locates the X/Open-compliant SQLState's class code.
	 *
	 * @param sqlException The exception from which to extract the SQLState class code
	 * @return The SQLState class code, or null.
	 */
	public static String extractSqlStateClassCode(SQLException sqlException) {
		return determineSqlStateClassCode( extractSqlState( sqlException ) );
	}

	public static String determineSqlStateClassCode(String sqlState) {
		if ( sqlState == null || sqlState.length() < 2 ) {
			return sqlState;
		}
		return sqlState.substring( 0, 2 );
	}
}
