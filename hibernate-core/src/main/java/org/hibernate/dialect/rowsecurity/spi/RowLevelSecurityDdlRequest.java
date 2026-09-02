/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Provides the rendered table and tenant-column facts needed to produce
/// row-level-security schema DDL.
///
/// Names returned by this request are already quoted for the active rendering
/// context and must be used verbatim. The request is read-only and call-scoped;
/// implementations must not retain it.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public interface RowLevelSecurityDdlRequest {
	/// Return the selected database-side tenant identifier source.
	TenantIdentifierSource tenantIdentifierSource();

	/// Return the qualified tenant-table name using mapped and configured
	/// namespace defaults.
	String qualifiedTableName();

	/// Return the qualified tenant-table name, applying `defaultSchema` only
	/// when neither a mapped nor configured schema is present.
	String qualifiedTableName(String defaultSchema);

	/// Qualify a sibling object in the tenant table's schema, applying
	/// `defaultSchema` only when neither a mapped nor configured schema is
	/// present.
	String qualifySiblingObject(String objectName, String defaultSchema);

	/// Return the stable boot-model export identifier of the tenant table.
	String tableExportIdentifier();

	/// Return the rendered tenant-column name.
	String tenantColumnName();

	/// Return the complete tenant-column SQL type declaration.
	String tenantColumnSqlType();

	/// Return the [org.hibernate.type.SqlTypes] code of the tenant column.
	int tenantColumnSqlTypeCode();
}
