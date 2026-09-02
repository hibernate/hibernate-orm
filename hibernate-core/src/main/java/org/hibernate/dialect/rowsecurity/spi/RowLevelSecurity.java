/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.spi;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Defines the database capabilities, schema DDL, and connection operation
/// used for database-native row-level security.
///
/// Implement this contract with immutable, thread-safe state. Inspect only the
/// ephemeral [RowLevelSecurityDdlRequest] supplied for the current table,
/// return declarative DDL descriptors, and do not retain the request.
///
/// @author Gavin King
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getRowLevelSecurity()
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface RowLevelSecurity {
	/// Report whether this strategy supports database-native row-level
	/// security.
	boolean supportsRowLevelSecurity();

	/// Report whether policies may obtain the tenant identifier from the
	/// specified database-side source.
	boolean supportsTenantIdentifierSource(TenantIdentifierSource source);

	/// Describe, in deterministic order, the schema commands required for the
	/// supplied tenant table. Use all rendered names in the request verbatim
	/// and return an immutable list.
	List<RowLevelSecurityDdl> getTenantTableDdl(RowLevelSecurityDdlRequest request);

	/// Apply the current tenant identifier to the JDBC connection. Implementations
	/// supporting only [TenantIdentifierSource#DATABASE_USER] may make this a
	/// no-op.
	void setTenantIdentifier(
			Connection connection,
			String tenantIdentifier,
			boolean root) throws SQLException;
}
