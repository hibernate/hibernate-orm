/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lob.spi;

import java.sql.DatabaseMetaData;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;

/// Defines the database and driver policy for LOB creation, binding,
/// materialization, type classification, mutation ordering, and VALUE LOB DDL.
///
/// Implementations must be stable and thread-safe. Do not retain JDBC metadata
/// or another call-scoped object passed to an operation.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getLobSupport()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface LobSupport {
	/// Determine whether JDBC Connection LOB factories may be used. Accept null
	/// metadata and do not retain it.
	default boolean supportsJdbcConnectionLobCreation(@Nullable DatabaseMetaData databaseMetaData) {
		return true;
	}

	/// Whether LOB values are bound using stream operations.
	default boolean useInputStreamToInsertBlob() {
		return true;
	}

	/// Whether LOB locators must be created using JDBC Connection factories.
	default boolean useConnectionToCreateLob() {
		return !useInputStreamToInsertBlob();
	}

	/// Whether the driver supports materialized byte and String LOB access.
	default boolean supportsMaterializedLobAccess() {
		return true;
	}

	/// Whether over-capacity varying mappings become materialized LOB mappings.
	default boolean useMaterializedLobWhenCapacityExceeded() {
		return supportsMaterializedLobAccess();
	}

	/// Whether LOB assignments must occur last in insert and update statements.
	default boolean forceLobAsLastValue() {
		return false;
	}

	/// Classify a JDBC or Hibernate SQL type code as a locator-oriented LOB.
	default boolean isLobType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case LONG32VARBINARY, LONG32VARCHAR, LONG32NVARCHAR, BLOB, CLOB, NCLOB -> true;
			default -> false;
		};
	}

	/// Render the complete column-specific VALUE LOB CREATE TABLE fragment, or
	/// return `null` when VALUE LOB DDL is unsupported.
	default @Nullable String getValueLobFragmentForExtraCreateTableInfo(String columnName) {
		if ( columnName == null ) {
			throw new IllegalArgumentException( "Column name must not be null" );
		}
		return null;
	}
}
