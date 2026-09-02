/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception.spi;

import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import jakarta.annotation.Nullable;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Converts database-specific SQL exceptions as one step in Hibernate's
/// exception-conversion chain.
///
/// Return `null` when this delegate does not recognize the exception. A null
/// result deliberately declines conversion and allows the remaining standard
/// delegates to inspect the same exception. Implementations must not throw or
/// manufacture a sentinel merely to indicate that they did not handle it.
///
/// Implementations should base conversion on stable vendor error codes, SQL
/// states, or exception subtypes and preserve the supplied SQL text in the
/// resulting [JDBCException] when applicable.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#buildSQLExceptionConversionDelegate()
@SPI({ USE, IMPLEMENT, SUPPLY })
@FunctionalInterface
public interface SQLExceptionConversionDelegate {
	/// Convert the given exception, or decline conversion by returning `null`.
	///
	/// @param sqlException the database exception to interpret
	/// @param message the message to use for a converted exception
	/// @param sql the SQL statement which caused the exception
	/// @return the converted exception, or `null` when this delegate does not
	/// recognize the exception
	@Nullable JDBCException convert(SQLException sqlException, String message, String sql);
}
