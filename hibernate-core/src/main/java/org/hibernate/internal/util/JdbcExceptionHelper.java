/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import java.sql.SQLException;

/**
 * @author Steve Ebersole
 */
public final class JdbcExceptionHelper {
	private JdbcExceptionHelper() {
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
