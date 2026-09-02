/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.jdbc.spi;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Extracts the unique result-column alias used while auto-discovering native
/// SQL query results.
///
/// Use [#COLUMN_LABEL_EXTRACTOR] for JDBC drivers which correctly report the
/// projected alias through [ResultSetMetaData#getColumnLabel]. Use
/// [#COLUMN_NAME_EXTRACTOR] only for drivers which require
/// [ResultSetMetaData#getColumnName] instead. The supplied position is the
/// one-based JDBC column position and must be forwarded unchanged.
///
/// Implementations must propagate [SQLException] so Hibernate can apply its
/// normal JDBC exception handling.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getColumnAliasExtractor()
@SPI({ USE, IMPLEMENT, SUPPLY })
@FunctionalInterface
public interface ColumnAliasExtractor {
	/// Extract the alias at the given one-based JDBC column position.
	///
	/// @param metaData the result-set metadata
	/// @param position the one-based JDBC column position
	/// @return the reported alias
	/// @throws SQLException when the driver cannot access the metadata
	String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException;

	/// The stock extractor which calls
	/// [ResultSetMetaData#getColumnLabel].
	ColumnAliasExtractor COLUMN_LABEL_EXTRACTOR = ResultSetMetaData::getColumnLabel;

	/// The stock extractor which calls [ResultSetMetaData#getColumnName].
	@SuppressWarnings("unused")
	ColumnAliasExtractor COLUMN_NAME_EXTRACTOR = ResultSetMetaData::getColumnName;
}
