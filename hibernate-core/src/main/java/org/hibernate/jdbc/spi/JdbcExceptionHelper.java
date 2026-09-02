/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jdbc.spi;

import java.sql.SQLException;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Inspects a JDBC exception chain for vendor error information.
///
/// JDBC drivers sometimes report the useful error code or SQL state on a
/// nested exception reached through [SQLException#getNextException()]. These
/// operations return the first usable value in that JDBC exception chain.
/// They do not inspect the general Java cause chain.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public final class JdbcExceptionHelper {
	private JdbcExceptionHelper() {
	}

	/// Finds the first nonzero vendor error code in the JDBC exception chain.
	///
	/// @param sqlException the first exception in the chain
	/// @return the first nonzero error code, or zero when every error code is zero
	/// @since 8.0
	@SPI(USE)
	public static int extractErrorCode(SQLException sqlException) {
		int errorCode = sqlException.getErrorCode();
		SQLException nested = sqlException.getNextException();
		while ( errorCode == 0 && nested != null ) {
			errorCode = nested.getErrorCode();
			nested = nested.getNextException();
		}
		return errorCode;
	}

	/// Finds the first non-null SQL state in the JDBC exception chain.
	///
	/// @param sqlException the first exception in the chain
	/// @return the first SQL state, or `null` when none is available
	/// @since 8.0
	@SPI(USE)
	public static @Nullable String extractSqlState(SQLException sqlException) {
		String sqlState = sqlException.getSQLState();
		SQLException nested = sqlException.getNextException();
		while ( sqlState == null && nested != null ) {
			sqlState = nested.getSQLState();
			nested = nested.getNextException();
		}
		return sqlState;
	}

	/// Finds the first SQL state and returns its two-character class code.
	///
	/// @param sqlException the first exception in the chain
	/// @return the SQL-state class code, or `null` when no state is available
	/// @since 8.0
	@SPI(USE)
	public static @Nullable String extractSqlStateClassCode(SQLException sqlException) {
		return determineSqlStateClassCode( extractSqlState( sqlException ) );
	}

	/// Returns the two-character class code of a SQL state.
	///
	/// A null or shorter value is returned unchanged.
	///
	/// @param sqlState a SQL state, or `null`
	/// @return its class code, or the unchanged null or short value
	/// @since 8.0
	@SPI(USE)
	public static @Nullable String determineSqlStateClassCode(@Nullable String sqlState) {
		return sqlState == null || sqlState.length() < 2 ? sqlState : sqlState.substring( 0, 2 );
	}
}
