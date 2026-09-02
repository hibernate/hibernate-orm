/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import java.sql.Connection;
import java.sql.SQLException;

import jakarta.annotation.Nullable;
import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Resolves the current schema name for a JDBC connection.
///
/// Implement this contract only when [Connection#getSchema()] is unavailable
/// or does not represent the database's schema semantics. Do not close the
/// supplied connection. Return `null` when no schema can be determined, and
/// preserve an empty string when it is the database's deliberate result.
///
/// @author Steve Ebersole
/// @see Dialect#getSchemaNameResolver()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface SchemaNameResolver {
	/// Resolve the current schema name without closing `connection`.
	@Nullable String resolveSchemaName(Connection connection, Dialect dialect) throws SQLException;
}
